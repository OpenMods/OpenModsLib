package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.ExecutionErrorException;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
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
import openmods.calc.parsing.SymbolGetNode;
import openmods.utils.Stack;

public class LetExpressionFactory {

	private final TypeDomain domain;
	private final String letSymbolName;
	private final String listSymbolName;
	private final BinaryOperator<TypedValue> colonOperator;

	public LetExpressionFactory(TypeDomain domain, String letSymbolName, String listSymbolName, BinaryOperator<TypedValue> colonOperator) {
		this.domain = domain;
		this.letSymbolName = letSymbolName;
		this.listSymbolName = listSymbolName;
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
				output.add(new SymbolCall<TypedValue>(listSymbolName, argumentCount, 1));
			} else { // assume list of arg pairs
				argsNode.flatten(output);
			}

			output.add(Value.create(Code.flattenAndWrap(domain, codeNode)));
			output.add(new SymbolCall<TypedValue>(letSymbolName, 2, 1));
		}

		private void flattenArgNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> argNode) {
			if (argNode instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> opNode = (BinaryOpNode<TypedValue>)argNode;
				if (opNode.operator == colonOperator) {
					flattenArgNameNode(output, opNode.left);
					output.add(Value.create(Code.flattenAndWrap(domain, opNode.right)));
					output.add(colonOperator);
					return;
				}
			}
			argNode.flatten(output); // not directly arg pair, but may still produce valid one
		}

		private void flattenArgNameNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> argNameNode) {
			if (argNameNode instanceof SymbolGetNode) {
				final SymbolGetNode<TypedValue> argSymbolNode = (SymbolGetNode<TypedValue>)argNameNode;
				final String symbolId = argSymbolNode.symbol();
				output.add(Value.create(Symbol.get(domain, symbolId)));
			} else {
				// either something we have to call or expression resulting in symbol
				argNameNode.flatten(output);
			}
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

	public ICallable<TypedValue> createSymbol() {
		return new LetSymbol();
	}
}
