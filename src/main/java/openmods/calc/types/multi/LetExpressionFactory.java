package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.ExecutionErrorException;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SquareBracketContainerNode;
import openmods.calc.parsing.SymbolCallNode;
import openmods.utils.Stack;

public class LetExpressionFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;
	private final BinaryOperator<TypedValue> colonOperator;

	public LetExpressionFactory(TypeDomain domain, TypedValue nullValue, BinaryOperator<TypedValue> colonOperator) {
		this.domain = domain;
		this.nullValue = nullValue;
		this.colonOperator = colonOperator;
	}

	private class LetNode implements IExprNode<TypedValue> {
		private final IExprNode<TypedValue> argsNode;
		private final IExprNode<TypedValue> codeNode;

		public LetNode(IExprNode<TypedValue> argsNode, IExprNode<TypedValue> codeNode) {
			this.argsNode = argsNode;
			this.codeNode = codeNode;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			// expecting [a:b,c:1+2]. If correctly formed, arg name (symbol) will be transformed into symbol atom
			if (argsNode instanceof SquareBracketContainerNode) {
				final SquareBracketContainerNode<TypedValue> bracketNode = (SquareBracketContainerNode<TypedValue>)argsNode;

				int argumentCount = 0;
				for (IExprNode<TypedValue> argNode : bracketNode.getChildren()) {
					flattenArgNode(output, argNode);
					argumentCount++;
				}

				Preconditions.checkState(argumentCount > 0, "'let' expects at least one argument");
				// slighly inefficient, but compatible with hand-called instruction
				output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, argumentCount, 1));
			} else { // assume list of arg pairs
				argsNode.flatten(output);
			}

			output.add(Value.create(Code.flattenAndWrap(domain, codeNode)));
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LET, 2, 1));
		}

		private void flattenArgNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> argNode) {
			if (argNode instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> opNode = (BinaryOpNode<TypedValue>)argNode;
				if (opNode.operator == colonOperator) {
					flattenNameAndValue(output, opNode.left, opNode.right);
					output.add(colonOperator);
					return;
				}
			}
			argNode.flatten(output); // not directly arg pair, but may still produce valid one
		}

		private void flattenNameAndValue(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> name, IExprNode<TypedValue> value) {
			if (name instanceof SymbolCallNode) {
				// f(x, y):<some code> -> f:(x,y)-><some code>
				final SymbolCallNode<TypedValue> callNode = (SymbolCallNode<TypedValue>)name;
				output.add(Value.create(Symbol.get(domain, callNode.symbol())));
				output.add(Value.create(createLambdaWrapperCode(callNode, value)));
			} else {
				try {
					// f:<some code>, 'f':<some code>, #f:<some code>
					output.add(Value.create(TypedCalcUtils.extractNameFromNode(domain, name)));
				} catch (IllegalArgumentException e) {
					// hopefully something that evaluates to symbol
					// TODO no valid syntax in prefix
					name.flatten(output);
				}
				output.add(flattenExprToCodeConstant(value));
			}
		}

		private TypedValue createLambdaWrapperCode(SymbolCallNode<TypedValue> callNode, IExprNode<TypedValue> value) {
			final List<IExecutable<TypedValue>> result = Lists.newArrayList();

			final List<TypedValue> argNames;
			try {
				argNames = extractArgNames(callNode.getChildren());
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Cannot extract lambda arg names from " + callNode);
			}
			result.add(Value.create(Cons.createList(argNames, nullValue)));
			result.add(flattenExprToCodeConstant(value));
			result.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE, 2, 1));
			return Code.wrap(domain, result);
		}

		private List<TypedValue> extractArgNames(Iterable<IExprNode<TypedValue>> children) {
			final List<TypedValue> result = Lists.newArrayList();
			for (IExprNode<TypedValue> child : children)
				result.add(TypedCalcUtils.extractNameFromNode(domain, child));
			return result;
		}

		private IExecutable<TypedValue> flattenExprToCodeConstant(IExprNode<TypedValue> code) {
			return Value.create(Code.flattenAndWrap(domain, code));
		}

		@Override
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return ImmutableList.of(argsNode, codeNode);
		}

	}

	private class LetStateTransition extends SameStateSymbolTransition<TypedValue> {
		public LetStateTransition(ICompilerState<TypedValue> parentState) {
			super(parentState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkState(children.size() == 2, "Expected two args for 'let' expression");
			return new LetNode(children.get(0), children.get(1));
		}
	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(parentState);
	}

	private static TypedValue calculateBindValue(final Frame<TypedValue> currentFrame, final Symbol name, final Code expr) {
		class PlaceholderSymbol implements ISymbol<TypedValue> {
			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				throw new ExecutionErrorException("Cannot call " + name.value + " symbol during definition");
			}

			@Override
			public void get(Frame<TypedValue> frame) {
				throw new ExecutionErrorException("Cannot reference " + name.value + " symbol during definition");
			}
		}

		final Frame<TypedValue> overlayFrame = FrameFactory.newLocalFrameWithSubstack(currentFrame, 0);
		overlayFrame.symbols().put(name.value, new PlaceholderSymbol());

		expr.execute(overlayFrame);

		final Stack<TypedValue> resultStack = overlayFrame.stack();
		Preconditions.checkState(resultStack.size() == 1, "Expected single result from 'let' expression, got %s", resultStack.size());
		final TypedValue result = resultStack.pop();
		overlayFrame.symbols().put(name.value, result); // replace placeholder with actual value
		return result;
	}

	private class LetSymbol extends FixedCallable<TypedValue> {

		public LetSymbol() {
			super(2, 1);
		}

		@Override
		public void call(final Frame<TypedValue> currentFrame) {
			final TypedValue code = currentFrame.stack().pop();

			final Frame<TypedValue> letFrame = FrameFactory.newLocalFrameWithSubstack(currentFrame, 1);
			final TypedValue paramPairs = letFrame.stack().pop();
			Preconditions.checkState(paramPairs.is(Cons.class), "Expected list of name:value pairs on first 'let' parameter, got %s", paramPairs);

			paramPairs.as(Cons.class).visit(new Cons.LinearVisitor() {
				@Override
				public void value(TypedValue value, boolean isLast) {
					Preconditions.checkState(value.is(Cons.class), "Expected list of name:value pairs on first 'let' parameter, got %s", paramPairs);
					final Cons pair = value.as(Cons.class);

					Preconditions.checkState(pair.car.is(Symbol.class), "Expected list of name:value pairs on first 'let' parameter, got %s", paramPairs);
					final Symbol name = pair.car.as(Symbol.class);

					Preconditions.checkState(pair.cdr.is(Code.class), "Expected list of name:value pairs on first 'let' parameter, got %s", paramPairs);
					final Code valueExpr = pair.cdr.as(Code.class);

					final TypedValue bindValue = calculateBindValue(currentFrame, name, valueExpr);
					letFrame.symbols().put(name.value, bindValue);
				}

				@Override
				public void end(TypedValue terminator) {}

				@Override
				public void begin() {}
			});

			code.as(Code.class, "second 'let' parameter").execute(letFrame);
		}
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LET, new LetSymbol());
	}
}
