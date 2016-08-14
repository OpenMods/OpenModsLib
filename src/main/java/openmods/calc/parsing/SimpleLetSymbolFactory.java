package openmods.calc.parsing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import openmods.calc.BinaryOperator;
import openmods.calc.Constant;
import openmods.calc.ExprType;
import openmods.calc.FunctionSymbol;
import openmods.calc.ICalculatorFrame;
import openmods.calc.ICompilerMapFactory;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalFrame;
import openmods.calc.StackValidationException;
import openmods.calc.ValueSymbol;
import openmods.utils.Stack;

public class SimpleLetSymbolFactory<E> {

	public static final String SYMBOL_LET = "let";

	public class ExtendedCompilerMapFactory extends BasicCompilerMapFactory<E> {
		@Override
		protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
			return SquareBracketContainerNode.install(new MappedExprNodeFactory<E>(valueParser));
		}

		@Override
		protected void configureCompilerStateCommon(MappedCompilerState<E> compilerState) {
			super.configureCompilerStateCommon(compilerState);
			compilerState.addStateTransition(SYMBOL_LET, createParserTransition(compilerState));
		}
	}

	private static class KeyValueSeparator<E> extends BinaryOperator<E> {
		private KeyValueSeparator(String id, int precendence) {
			super(id, precendence);
		}

		@Override
		public E execute(E left, E right) {
			throw new UnsupportedOperationException(); // not supposed to be used directly;
		}
	}

	private final BinaryOperator<E> keyValueSeparator;

	public SimpleLetSymbolFactory(String keyValueSeparatorId, int keyValueSeparatorPriority) {
		this.keyValueSeparator = new KeyValueSeparator<E>(keyValueSeparatorId, keyValueSeparatorPriority);
	}

	public BinaryOperator<E> getKeyValueSeparator() {
		return keyValueSeparator;
	}

	private interface ISymbolBinder<E> {
		public ISymbol<E> bind(ICalculatorFrame<E> frame);
	}

	private static <E> ISymbolBinder<E> createLetLazyConstant(IExprNode<E> valueNode) {
		final IExecutable<E> exprExecutable = ExprUtils.flattenNode(valueNode);

		return new ISymbolBinder<E>() {
			@Override
			public ISymbol<E> bind(final ICalculatorFrame<E> bindSiteFrame) {
				class LazyConstant extends ValueSymbol<E> {
					private boolean isEvaluated;
					private E value;

					@Override
					public void get(ICalculatorFrame<E> callSiteFrame) {
						if (!isEvaluated) {
							final ICalculatorFrame<E> executionFrame = new LocalFrame<E>(bindSiteFrame);
							exprExecutable.execute(executionFrame);
							final Stack<E> resultStack = executionFrame.stack();
							Preconditions.checkState(resultStack.size() == 1, "Expected one value from let expression, got %s", resultStack.size());
							value = resultStack.pop();
						}

						callSiteFrame.stack().push(value);
					}
				}

				return new LazyConstant();
			}
		};
	}

	private static <E> ISymbolBinder<E> createLetFunction(SymbolCallNode<E> symbolCallNode, IExprNode<E> valueNode) {
		List<String> args = Lists.newArrayList();
		for (IExprNode<E> arg : symbolCallNode.getChildren()) {
			Preconditions.checkState(arg instanceof SymbolGetNode, "Expected symbol, got %s", arg);
			args.add(((SymbolGetNode<E>)arg).symbol());
		}

		final List<String> reversedArgs = Lists.reverse(args);
		final IExecutable<E> exprExecutable = ExprUtils.flattenNode(valueNode);

		return new ISymbolBinder<E>() {
			@Override
			public ISymbol<E> bind(final ICalculatorFrame<E> bindSiteFrame) {
				class LetFunction extends FunctionSymbol<E> {

					@Override
					public void call(ICalculatorFrame<E> callSiteFrame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
						if (argumentsCount.isPresent()) {
							final int givenArgCount = argumentsCount.get();
							final int expectedArgCount = reversedArgs.size();
							if (givenArgCount != expectedArgCount) throw new StackValidationException("Expected %s argument(s) but got %s", expectedArgCount, givenArgCount);
						}

						final LocalFrame<E> executionFrame = new LocalFrame<E>(bindSiteFrame);
						for (String arg : reversedArgs)
							executionFrame.setLocalSymbol(arg, Constant.create(callSiteFrame.stack().pop()));

						exprExecutable.execute(executionFrame);

						if (returnsCount.isPresent()) {
							final int expectedReturns = returnsCount.get();
							final int actualReturns = executionFrame.stack().size();
							if (expectedReturns != actualReturns) throw new StackValidationException("Has %s result(s) but expected %s", actualReturns, expectedReturns);
						}

						for (E ret : executionFrame.stack())
							callSiteFrame.stack().push(ret);
					}

				}

				return new LetFunction();
			}
		};

	}

	private class LetExecutable implements IExecutable<E> {

		private final Map<String, ISymbolBinder<E>> variables;

		private final IExecutable<E> expr;

		public LetExecutable(Map<String, ISymbolBinder<E>> variables, IExecutable<E> expr) {
			this.variables = variables;
			this.expr = expr;
		}

		@Override
		public void execute(ICalculatorFrame<E> frame) {
			final LocalFrame<E> letFrame = new LocalFrame<E>(frame);

			for (Map.Entry<String, ISymbolBinder<E>> e : variables.entrySet())
				letFrame.setLocalSymbol(e.getKey(), e.getValue().bind(frame));

			expr.execute(letFrame);

			for (E result : letFrame.stack())
				frame.stack().push(result);
		}

		@Override
		public String serialize() {
			return "<let>"; // unserializable in most calculators
		}

	}

	private class LetNode implements IExprNode<E> {
		private final IExprNode<E> argsNode;
		private final IExprNode<E> codeNode;

		public LetNode(IExprNode<E> argsNode, IExprNode<E> codeNode) {
			this.argsNode = argsNode;
			this.codeNode = codeNode;
		}

		@Override
		public void flatten(List<IExecutable<E>> output) {
			// expecting [a:b:,c:1+2]. If correctly formed, arg name (symbol) will be transformed into symbol atom
			Preconditions.checkState(argsNode instanceof SquareBracketContainerNode, "Malformed 'let' expressions: expected brackets, got %s", argsNode);
			final SquareBracketContainerNode<E> bracketNode = (SquareBracketContainerNode<E>)argsNode;
			final ImmutableMap<String, ISymbolBinder<E>> vars = collectVars(bracketNode);
			final IExecutable<E> code = ExprUtils.flattenNode(codeNode);
			output.add(new LetExecutable(vars, code));
		}

		private ImmutableMap<String, ISymbolBinder<E>> collectVars(final SquareBracketContainerNode<E> bracketNode) {
			final ImmutableMap.Builder<String, ISymbolBinder<E>> varsBuilder = ImmutableMap.builder();
			for (IExprNode<E> argNode : bracketNode.getChildren())
				flattenArgNode(varsBuilder, argNode);
			return varsBuilder.build();
		}

		private void flattenArgNode(ImmutableMap.Builder<String, ISymbolBinder<E>> output, IExprNode<E> argNode) {
			Preconditions.checkState(argNode instanceof BinaryOpNode, "Expected expression in from <name>:<expr>, got %s", argNode);
			final BinaryOpNode<E> opNode = (BinaryOpNode<E>)argNode;
			Preconditions.checkState(opNode.operator == keyValueSeparator, "Expected operator %s as separator, got %s", keyValueSeparator.id, opNode.operator.id);

			final IExprNode<E> nameNode = opNode.left;
			final IExprNode<E> valueExprNode = opNode.right;
			if (nameNode instanceof SymbolGetNode) {
				final SymbolGetNode<E> symbolGetNode = (SymbolGetNode<E>)nameNode;
				output.put(symbolGetNode.symbol(), createLetLazyConstant(valueExprNode));
			} else if (nameNode instanceof SymbolCallNode) {
				final SymbolCallNode<E> symbolCallNode = (SymbolCallNode<E>)nameNode;
				output.put(symbolCallNode.symbol(), createLetFunction(symbolCallNode, valueExprNode));
			} else {
				throw new IllegalStateException("Expected symbol, got " + nameNode);
			}
		}

		@Override
		public Iterable<IExprNode<E>> getChildren() {
			return ImmutableList.of(argsNode, codeNode);
		}
	}

	public ISymbolCallStateTransition<E> createParserTransition(final ICompilerState<E> currentState) {
		return new ISymbolCallStateTransition<E>() {

			@Override
			public ICompilerState<E> getState() {
				return currentState;
			}

			@Override
			public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
				Preconditions.checkState(children.size() == 2, "Expected two args for 'let' expression");
				return new LetNode(children.get(0), children.get(1));
			}
		};
	}

	public ICompilerMapFactory<E, ExprType> createCompilerFactory() {
		return new ExtendedCompilerMapFactory();
	}
}
