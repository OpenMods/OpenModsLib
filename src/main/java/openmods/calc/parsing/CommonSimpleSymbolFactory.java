package openmods.calc.parsing;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import openmods.calc.Environment;
import openmods.calc.ExecutionErrorException;
import openmods.calc.ExprType;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICompilerMapFactory;
import openmods.calc.executable.BinaryOperator;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.Operator;
import openmods.calc.executable.OperatorDictionary;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.ISymbolCallStateTransition;
import openmods.calc.parsing.ast.MappedParserState;
import openmods.calc.parsing.ast.SingleStateTransition;
import openmods.calc.parsing.node.BinaryOpNode;
import openmods.calc.parsing.node.DefaultExprNodeFactory;
import openmods.calc.parsing.node.ExprUtils;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.MappedExprNodeFactory;
import openmods.calc.parsing.node.SingleExecutableNode;
import openmods.calc.parsing.node.SquareBracketContainerNode;
import openmods.calc.parsing.node.SymbolCallNode;
import openmods.calc.parsing.node.SymbolGetNode;
import openmods.calc.parsing.token.Token;
import openmods.calc.symbol.ICallable;
import openmods.calc.symbol.IGettable;
import openmods.calc.symbol.SymbolMap;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;
import openmods.utils.StackValidationException;

public class CommonSimpleSymbolFactory<E> {

	public static final String SYMBOL_LET = "let";
	public static final String SYMBOL_FAIL = "fail";
	public static final String SYMBOL_CONSTANT = "const";

	private class FailStateTransition extends SingleStateTransition.ForSymbol<IExprNode<E>> {
		@Override
		public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
			Preconditions.checkState(children.size() <= 1, "'fail' expects at most single argument, got %s", children.size());
			if (children.isEmpty()) {
				return new SingleExecutableNode<E>(new IExecutable<E>() {
					@Override
					public void execute(Frame<E> frame) {
						throw new ExecutionErrorException();
					}
				});
			} else {
				return children.get(0);
			}
		}

		@Override
		public IExprNode<E> parseSymbol(IParserState<IExprNode<E>> state, PeekingIterator<Token> input) {
			final Token arg = input.next();
			final String failCause = arg.value;
			return new SingleExecutableNode<E>(new IExecutable<E>() {
				@Override
				public void execute(Frame<E> frame) {
					throw new ExecutionErrorException(failCause);
				}
			});
		}
	}

	public class ExtendedCompilerMapFactory extends BasicCompilerMapFactory<E> {
		@Override
		protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
			return SquareBracketContainerNode.install(new MappedExprNodeFactory<E>(valueParser));
		}

		@Override
		protected void configureCompilerStateCommon(MappedParserState<IExprNode<E>> compilerState, Environment<E> environment) {
			super.configureCompilerStateCommon(compilerState, environment);
			compilerState.addStateTransition(SYMBOL_LET, createParserTransition(compilerState));
			compilerState.addStateTransition(SYMBOL_FAIL, new FailStateTransition());
			compilerState.addStateTransition(SYMBOL_CONSTANT, new ConstantSymbolStateTransition<E>(compilerState, environment, SYMBOL_CONSTANT));
		}
	}

	private static class KeyValueSeparator<E> extends BinaryOperator.Direct<E> {
		private KeyValueSeparator(String id, int precendence) {
			super(id, precendence);
		}

		@Override
		public E execute(E left, E right) {
			throw new UnsupportedOperationException(); // not supposed to be used directly;
		}
	}

	private final Set<BinaryOperator<E>> keyValueSeparators;

	private final String keyValueSeparatorsIds;

	public CommonSimpleSymbolFactory(int keyValueSeparatorPriority, String... keyValueSeparatorIds) {
		final ImmutableSet.Builder<BinaryOperator<E>> separators = ImmutableSet.builder();
		for (String opId : keyValueSeparatorIds)
			separators.add(new KeyValueSeparator<E>(opId, keyValueSeparatorPriority));
		this.keyValueSeparators = separators.build();
		this.keyValueSeparatorsIds = Joiner.on(',').join(keyValueSeparatorIds);
	}

	public Collection<BinaryOperator<E>> getKeyValueSeparators() {
		return keyValueSeparators;
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
					public E get() {
						if (!isEvaluated) {
							final Frame<E> executionFrame = FrameFactory.newLocalFrame(enclosingFrame);
							exprExecutable.execute(executionFrame);
							value = executionFrame.stack().popAndExpectEmptyStack();
						}

						return value;
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
					public void call(Frame<E> callSiteFrame, OptionalInt argumentsCount, OptionalInt returnsCount) {
						final int expectedArgCount = reversedArgs.size();
						if (!argumentsCount.compareIfPresent(expectedArgCount)) throw new StackValidationException("Expected %s argument(s) but got %s", expectedArgCount, argumentsCount.get());

						final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(enclosingFrame, expectedArgCount);
						final Stack<E> argStack = executionFrame.stack();
						for (String arg : reversedArgs)
							executionFrame.symbols().put(arg, argStack.pop());

						exprExecutable.execute(executionFrame);

						final int actualReturns = argStack.size();
						if (!returnsCount.compareIfPresent(actualReturns)) throw new StackValidationException("Has %s result(s) but expected %s", actualReturns, returnsCount.get());
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
			Preconditions.checkState(keyValueSeparators.contains(opNode.operator), "Expected operators %s as separator, got %s", keyValueSeparatorsIds, opNode.operator.id);

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

	public ISymbolCallStateTransition<IExprNode<E>> createParserTransition(final IParserState<IExprNode<E>> currentState) {
		return new ISymbolCallStateTransition<IExprNode<E>>() {

			@Override
			public IParserState<IExprNode<E>> getState() {
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

	public void registerSeparators(OperatorDictionary<Operator<E>> operators) {
		for (BinaryOperator<E> separator : keyValueSeparators)
			operators.registerOperator(separator);
	}

}
