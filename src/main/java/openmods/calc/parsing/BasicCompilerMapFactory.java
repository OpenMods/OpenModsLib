package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import java.util.Map;
import openmods.calc.Compilers;
import openmods.calc.Compilers.ICompiler;
import openmods.calc.Environment;
import openmods.calc.ExecutableList;
import openmods.calc.ExprType;
import openmods.calc.Frame;
import openmods.calc.ICompilerMapFactory;
import openmods.calc.IExecutable;
import openmods.calc.OperatorDictionary;
import openmods.calc.Value;
import openmods.calc.parsing.DefaultPostfixCompiler.IStateProvider;

public class BasicCompilerMapFactory<E> implements ICompilerMapFactory<E, ExprType> {

	public static final String BRACKET_CONSTANT_EVALUATE = "[";
	public static final String MODIFIER_SYMBOL_GET = "@";
	public static final String SYMBOL_PREFIX = "prefix";
	public static final String SYMBOL_INFIX = "infix";

	private static class WrappedCompiler<E> implements ICompiler<E> {
		private final Tokenizer tokenizer;
		private final ITokenStreamCompiler<E> compiler;

		private WrappedCompiler(Tokenizer tokenizer, ITokenStreamCompiler<E> compiler) {
			this.tokenizer = tokenizer;
			this.compiler = compiler;
		}

		@Override
		public IExecutable<E> compile(String input) {
			final PeekingIterator<Token> tokens = tokenizer.tokenize(input);
			final IExecutable<E> result = compiler.compile(tokens);
			if (tokens.hasNext())
				throw new IllegalStateException("Unconsumed tokens: " + Lists.newArrayList(tokens));

			return result;
		}
	}

	public static class ParserSwitchTransition<E> implements ISymbolCallStateTransition<E> {
		private ICompilerState<E> switchState;

		public ParserSwitchTransition(ICompilerState<E> switchState) {
			this.switchState = switchState;
		}

		@Override
		public ICompilerState<E> getState() {
			return switchState;
		}

		@Override
		public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
			Preconditions.checkState(children.size() == 1, "Expected one node, got %s", children);
			return new DummyNode<E>(children.get(0));
		}
	}

	@Override
	public Compilers<E, ExprType> create(E nullValue, IValueParser<E> valueParser, OperatorDictionary<E> operators, Environment<E> environment) {
		final Tokenizer prefixTokenizer = new Tokenizer();

		final Tokenizer infixTokenizer = new Tokenizer();

		final Tokenizer postfixTokenizer = new Tokenizer();

		for (String operator : operators.allOperators()) {
			prefixTokenizer.addOperator(operator);
			infixTokenizer.addOperator(operator);
			postfixTokenizer.addOperator(operator);
		}

		setupPrefixTokenizer(prefixTokenizer);
		setupInfixTokenizer(infixTokenizer);
		setupPostfixTokenizer(postfixTokenizer);

		final IExprNodeFactory<E> exprNodeFactory = createExprNodeFactory(valueParser);
		final MappedCompilerState<E> prefixCompilerState = createPrefixCompilerState(operators, exprNodeFactory);
		final MappedCompilerState<E> infixCompilerState = createInfixCompilerState(operators, exprNodeFactory);

		prefixCompilerState.addStateTransition(SYMBOL_INFIX, new ParserSwitchTransition<E>(infixCompilerState));
		prefixCompilerState.addStateTransition(SYMBOL_PREFIX, new ParserSwitchTransition<E>(prefixCompilerState));

		infixCompilerState.addStateTransition(SYMBOL_INFIX, new ParserSwitchTransition<E>(infixCompilerState));
		infixCompilerState.addStateTransition(SYMBOL_PREFIX, new ParserSwitchTransition<E>(prefixCompilerState));

		configureCompilerStateCommon(prefixCompilerState, environment);
		configureCompilerStateCommon(infixCompilerState, environment);

		final Map<ExprType, ICompiler<E>> compilers = Maps.newHashMap();
		compilers.put(ExprType.PREFIX, new WrappedCompiler<E>(prefixTokenizer, createPrefixParser(prefixCompilerState)));
		compilers.put(ExprType.INFIX, new WrappedCompiler<E>(infixTokenizer, createInfixParser(infixCompilerState)));
		compilers.put(ExprType.POSTFIX, new WrappedCompiler<E>(postfixTokenizer, createPostfixParser(valueParser, operators, environment)));
		return new Compilers<E, ExprType>(compilers);
	}

	protected void setupPrefixTokenizer(Tokenizer tokenizer) {}

	protected MappedCompilerState<E> createPrefixCompilerState(OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		final IAstParser<E> prefixParser = new PrefixParser<E>(operators, exprNodeFactory);
		return new MappedCompilerState<E>(prefixParser);
	}

	protected ITokenStreamCompiler<E> createPrefixParser(ICompilerState<E> compilerState) {
		return new AstCompiler<E>(compilerState);
	}

	protected void setupInfixTokenizer(Tokenizer tokenizer) {}

	protected MappedCompilerState<E> createInfixCompilerState(OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		final IAstParser<E> infixParser = new InfixParser<E>(operators, exprNodeFactory);
		return new MappedCompilerState<E>(infixParser);
	}

	protected ITokenStreamCompiler<E> createInfixParser(ICompilerState<E> compilerState) {
		return new AstCompiler<E>(compilerState);
	}

	protected void setupPostfixTokenizer(Tokenizer tokenizer) {
		tokenizer.addModifier(MODIFIER_SYMBOL_GET);
	}

	protected ITokenStreamCompiler<E> createPostfixParser(IValueParser<E> valueParser, OperatorDictionary<E> operators, final Environment<E> env) {
		final DefaultPostfixCompiler<E> compiler = new DefaultPostfixCompiler<E>(valueParser, operators);
		return addSymbolGetState(addConstantEvaluatorState(valueParser, operators, env, compiler));
	}

	public static <E> DefaultPostfixCompiler<E> addConstantEvaluatorState(IValueParser<E> valueParser, OperatorDictionary<E> operators, Environment<E> env, DefaultPostfixCompiler<E> compiler) {
		return compiler.addBracketStateProvider(BRACKET_CONSTANT_EVALUATE, createConstantEvaluatorStateProvider(valueParser, operators, env, BRACKET_CONSTANT_EVALUATE));
	}

	public static <E> IStateProvider<E> createConstantEvaluatorStateProvider(final IValueParser<E> valueParser, final OperatorDictionary<E> operators, final Environment<E> env, final String openingBracket) {
		return new IStateProvider<E>() {
			@Override
			public IPostfixCompilerState<E> createState() {
				final IExecutableListBuilder<E> listBuilder = new DefaultExecutableListBuilder<E>(valueParser, operators);

				class ConstantEvaluatorState extends BracketPostfixCompilerStateBase<E> {
					ConstantEvaluatorState() {
						super(listBuilder, openingBracket);
					}

					@Override
					protected IExecutable<E> processCompiledBracket(final IExecutable<E> compiledExpr) {
						final Frame<E> resultFrame = env.executeIsolated(compiledExpr);
						final List<IExecutable<E>> computedValues = Lists.newArrayList();
						for (E value : resultFrame.stack())
							computedValues.add(Value.create(value));
						return ExecutableList.wrap(computedValues);
					}
				}
				return new ConstantEvaluatorState();
			}
		};
	}

	public static <E> DefaultPostfixCompiler<E> addSymbolGetState(DefaultPostfixCompiler<E> compiler) {
		return compiler.addModifierStateProvider(MODIFIER_SYMBOL_GET, new IStateProvider<E>() {
			@Override
			public IPostfixCompilerState<E> createState() {
				return new SymbolGetPostfixCompilerState<E>();
			}
		});
	}

	protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
		return new DefaultExprNodeFactory<E>(valueParser);
	}

	protected void configureCompilerStateCommon(MappedCompilerState<E> compilerState, Environment<E> environment) {}
}
