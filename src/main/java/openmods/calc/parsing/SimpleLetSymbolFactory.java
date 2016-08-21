package openmods.calc.parsing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.ExprType;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.ICompilerMapFactory;
import openmods.calc.IExecutable;
import openmods.calc.IGettable;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolMap;
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
		public void bind(SymbolMap<E> localSymbols, Frame<E> enclosingFrame);
	}

	private static <E> ISymbolBinder<E> createLetLazyConstant(final String name, IExprNode<E> valueNode) {
		final IExecutable<E> exprExecutable = ExprUtils.flattenNode(valueNode);

		return new ISymbolBinder<E>() {
			@Override
			public void bind(SymbolMap<E> localSymbols, final Frame<E> enclosingFrame) {
				class LazyConstant implements IGettable<E> {
					private boolean isEvaluated;
					private E value;

					@Override
					public void get(Frame<E> callSiteFrame) {
						if (!isEvaluated) {
							final Frame<E> executionFrame = FrameFactory.newLocalFrame(enclosingFrame);
							exprExecutable.execute(executionFrame);
							final Stack<E> resultStack = executionFrame.stack();
							Preconditions.checkState(resultStack.size() == 1, "Expected one value from let expression, got %s", resultStack.size());
							value = resultStack.pop();
						}

						callSiteFrame.stack().push(value);
					}
				}

				localSymbols.put(name, new LazyConstant());
			}
		};
	}

	private static <E> ISymbolBinder<E> createLetFunction(final String name, SymbolCallNode<E> symbolCallNode, IExprNode<E> valueNode) {
		List<String> args = Lists.newArrayList();
		for (IExprNode<E> arg : symbolCallNode.getChildren()) {
			Preconditions.checkState(arg instanceof SymbolGetNode, "Expected symbol, got %s", arg);
			args.add(((SymbolGetNode<E>)arg).symbol());
		}

		final List<String> reversedArgs = Lists.reverse(args);
		final IExecutable<E> exprExecutable = ExprUtils.flattenNode(valueNode);

		return new ISymbolBinder<E>() {
			@Override
			public void bind(SymbolMap<E> localSymbols, final Frame<E> enclosingFrame) {
				class LetFunction implements ICallable<E> {

					@Override
					public void call(Frame<E> callSiteFrame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
						final int expectedArgCount = reversedArgs.size();

						if (argumentsCount.isPresent()) {
							final int givenArgCount = argumentsCount.get();
							if (givenArgCount != expectedArgCount) throw new StackValidationException("Expected %s argument(s) but got %s", expectedArgCount, givenArgCount);
						}

						final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(enclosingFrame, expectedArgCount);
						final Stack<E> argStack = executionFrame.stack();
						for (String arg : reversedArgs)
							executionFrame.symbols().put(arg, argStack.pop());

						exprExecutable.execute(executionFrame);

						if (returnsCount.isPresent()) {
							final int expectedReturns = returnsCount.get();
							final int actualReturns = argStack.size();
							if (expectedReturns != actualReturns) throw new StackValidationException("Has %s result(s) but expected %s", actualReturns, expectedReturns);
						}
					}

				}

				localSymbols.put(name, new LetFunction());
			}
		};

	}

	private class LetExecutable implements IExecutable<E> {

		private final List<ISymbolBinder<E>> variables;

		private final IExecutable<E> expr;

		public LetExecutable(List<ISymbolBinder<E>> variables, IExecutable<E> expr) {
			this.variables = variables;
			this.expr = expr;
		}

		@Override
		public void execute(Frame<E> frame) {
			final Frame<E> letFrame = FrameFactory.newLocalFrameWithSubstack(frame, 0);

			for (ISymbolBinder<E> e : variables)
				e.bind(letFrame.symbols(), frame);

			expr.execute(letFrame);
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
			final ImmutableList<ISymbolBinder<E>> vars = collectVars(bracketNode);
			final IExecutable<E> code = ExprUtils.flattenNode(codeNode);
			output.add(new LetExecutable(vars, code));
		}

		private ImmutableList<ISymbolBinder<E>> collectVars(final SquareBracketContainerNode<E> bracketNode) {
			final ImmutableList.Builder<ISymbolBinder<E>> varsBuilder = ImmutableList.builder();
			for (IExprNode<E> argNode : bracketNode.getChildren())
				flattenArgNode(varsBuilder, argNode);
			return varsBuilder.build();
		}

		private void flattenArgNode(ImmutableList.Builder<ISymbolBinder<E>> output, IExprNode<E> argNode) {
			Preconditions.checkState(argNode instanceof BinaryOpNode, "Expected expression in from <name>:<expr>, got %s", argNode);
			final BinaryOpNode<E> opNode = (BinaryOpNode<E>)argNode;
			Preconditions.checkState(opNode.operator == keyValueSeparator, "Expected operator %s as separator, got %s", keyValueSeparator.id, opNode.operator.id);

			final IExprNode<E> nameNode = opNode.left;
			final IExprNode<E> valueExprNode = opNode.right;
			if (nameNode instanceof SymbolGetNode) {
				final SymbolGetNode<E> symbolGetNode = (SymbolGetNode<E>)nameNode;
				output.add(createLetLazyConstant(symbolGetNode.symbol(), valueExprNode));
			} else if (nameNode instanceof SymbolCallNode) {
				final SymbolCallNode<E> symbolCallNode = (SymbolCallNode<E>)nameNode;
				output.add(createLetFunction(symbolCallNode.symbol(), symbolCallNode, valueExprNode));
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
