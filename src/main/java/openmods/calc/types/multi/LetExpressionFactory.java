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
import openmods.calc.SymbolMap;
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
		private final String letSymbol;
		private final IExprNode<TypedValue> argsNode;
		private final IExprNode<TypedValue> codeNode;

		public LetNode(String letSymbol, IExprNode<TypedValue> argsNode, IExprNode<TypedValue> codeNode) {
			this.letSymbol = letSymbol;
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
			output.add(new SymbolCall<TypedValue>(letSymbol, 2, 1));
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
		private final String letState;

		public LetStateTransition(String letState, ICompilerState<TypedValue> parentState) {
			super(parentState);
			this.letState = letState;
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkState(children.size() == 2, "Expected two args for 'let' expression");
			return new LetNode(letState, children.get(0), children.get(1));
		}
	}

	public ISymbolCallStateTransition<TypedValue> createLetStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(TypedCalcConstants.SYMBOL_LET, parentState);
	}

	public ISymbolCallStateTransition<TypedValue> createLetSeqStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(TypedCalcConstants.SYMBOL_LETSEQ, parentState);
	}

	public ISymbolCallStateTransition<TypedValue> createLetRecStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(TypedCalcConstants.SYMBOL_LETREC, parentState);
	}

	private static class PlaceholderSymbol implements ISymbol<TypedValue> {
		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			throw new ExecutionErrorException("Cannot call symbol during definition");
		}

		@Override
		public TypedValue get() {
			throw new ExecutionErrorException("Cannot reference symbol during definition");
		}
	}

	@SuppressWarnings("serial")
	private static class InvalidArgsException extends RuntimeException {}

	private static abstract class LetSymbolBase extends FixedCallable<TypedValue> {

		public LetSymbolBase() {
			super(2, 1);
		}

		@Override
		public void call(Frame<TypedValue> currentFrame) {
			final Frame<TypedValue> letFrame = FrameFactory.newLocalFrameWithSubstack(currentFrame, 2);
			final TypedValue code = letFrame.stack().pop();
			Preconditions.checkState(code.is(Code.class), "Expected code of first 'let' parameter, got %s", code);

			final TypedValue paramPairs = letFrame.stack().pop();
			Preconditions.checkState(paramPairs.is(Cons.class), "Expected list of name:value pairs on second 'let' parameter, got %s", paramPairs);
			final Cons vars = paramPairs.as(Cons.class);

			try {
				prepareFrame(letFrame.symbols(), currentFrame.symbols(), vars);
			} catch (InvalidArgsException e) {
				throw new IllegalArgumentException("Expected list of name:value pairs on second 'let' parameter, got " + vars, e);
			}

			code.as(Code.class, "second 'let' parameter").execute(letFrame);
		}

		protected abstract void prepareFrame(SymbolMap<TypedValue> outputFrame, SymbolMap<TypedValue> callSymbols, Cons vars);
	}

	private abstract static class ArgPairVisitor implements Cons.LinearVisitor {
		@Override
		public void value(TypedValue value, boolean isLast) {
			if (!value.is(Cons.class)) throw new InvalidArgsException();
			final Cons pair = value.as(Cons.class);

			Preconditions.checkState(pair.car.is(Symbol.class));
			final Symbol name = pair.car.as(Symbol.class);

			if (!pair.cdr.is(Code.class)) throw new InvalidArgsException();
			final Code valueExpr = pair.cdr.as(Code.class);

			acceptVar(name, valueExpr);
		}

		protected abstract void acceptVar(Symbol name, Code value);

		@Override
		public void end(TypedValue terminator) {}

		@Override
		public void begin() {}
	}

	private class LetSymbol extends LetSymbolBase {
		@Override
		protected void prepareFrame(final SymbolMap<TypedValue> outputSymbols, final SymbolMap<TypedValue> callSymbols, Cons vars) {
			vars.visit(new ArgPairVisitor() {
				@Override
				protected void acceptVar(Symbol name, Code expr) {
					final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(callSymbols);
					executionFrame.symbols().put(name.value, new PlaceholderSymbol());

					expr.execute(executionFrame);

					final Stack<TypedValue> resultStack = executionFrame.stack();
					Preconditions.checkState(resultStack.size() == 1, "Expected single result from 'let' expression, got %s", resultStack.size());
					final TypedValue result = resultStack.pop();
					executionFrame.symbols().put(name.value, result); // replace placeholder with actual value
					outputSymbols.put(name.value, result);
				}
			});
		}
	}

	private class LetSeqSymbol extends LetSymbolBase {
		@Override
		protected void prepareFrame(final SymbolMap<TypedValue> outputSymbols, SymbolMap<TypedValue> callSymbols, Cons vars) {
			final Frame<TypedValue> executionFrame = FrameFactory.symbolsToFrame(outputSymbols);
			final Stack<TypedValue> resultStack = executionFrame.stack();

			vars.visit(new ArgPairVisitor() {
				@Override
				protected void acceptVar(Symbol name, Code expr) {
					outputSymbols.put(name.value, new PlaceholderSymbol());
					expr.execute(executionFrame);
					Preconditions.checkState(resultStack.size() == 1, "Expected single result from 'let' expression, got %s", resultStack.size());
					outputSymbols.put(name.value, resultStack.pop());
				}
			});
		}
	}

	private static class NameCodePair {
		public final String name;
		public final Code code;

		public NameCodePair(String name, Code code) {
			this.name = name;
			this.code = code;
		}
	}

	private static class NameValuePair {
		public final String name;
		public final TypedValue value;

		public NameValuePair(String name, TypedValue value) {
			this.name = name;
			this.value = value;
		}

	}

	private class LetRecSymbol extends LetSymbolBase {
		@Override
		protected void prepareFrame(final SymbolMap<TypedValue> outputSymbols, SymbolMap<TypedValue> callSymbols, Cons vars) {
			final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(callSymbols);
			final SymbolMap<TypedValue> executionSymbols = executionFrame.symbols();
			final List<NameCodePair> varsToExecute = Lists.newArrayList();

			// fill placeholders, collect data
			vars.visit(new ArgPairVisitor() {
				@Override
				protected void acceptVar(Symbol name, Code expr) {
					executionSymbols.put(name.value, new PlaceholderSymbol());
					varsToExecute.add(new NameCodePair(name.value, expr));
				}
			});

			final Stack<TypedValue> resultStack = executionFrame.stack();

			// evaluate expressions
			final List<NameValuePair> varsToSet = Lists.newArrayList();
			for (NameCodePair e : varsToExecute) {
				e.code.execute(executionFrame);
				Preconditions.checkState(resultStack.size() == 1, "Expected single result from 'let' expression, got %s", resultStack.size());
				final TypedValue result = resultStack.pop();
				varsToSet.add(new NameValuePair(e.name, result));
			}

			// expose results to namespace - must be done after evaluations, since all symbols must be executed with dummy values in place
			// IMO this is more consistent than "each id is initialized immediately after the corresponding val-expr is evaluated"
			for (NameValuePair e : varsToSet) {
				executionSymbols.put(e.name, e.value);
				outputSymbols.put(e.name, e.value);
			}
		}
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LET, new LetSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LETSEQ, new LetSeqSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LETREC, new LetRecSymbol());
	}
}
