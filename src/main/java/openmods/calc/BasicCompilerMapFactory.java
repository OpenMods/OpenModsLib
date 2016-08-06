package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import java.util.Map;
import openmods.calc.Compilers.ICompiler;
import openmods.calc.parsing.AstCompiler;
import openmods.calc.parsing.DefaultExecutableListBuilder;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.DefaultPostfixCompiler;
import openmods.calc.parsing.DefaultPostfixCompiler.IStateProvider;
import openmods.calc.parsing.DummyNode;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.ICompilerState.ISymbolStateTransition;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.IPostfixCompilerState;
import openmods.calc.parsing.ITokenStreamCompiler;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.InfixParser;
import openmods.calc.parsing.PrefixParser;
import openmods.calc.parsing.SimplePostfixCompilerState;
import openmods.calc.parsing.SymbolNode;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import openmods.calc.parsing.TokenUtils;
import openmods.calc.parsing.Tokenizer;

public class BasicCompilerMapFactory<E> implements ICompilerMapFactory<E, ExprType> {

	public static final String BRACKET_CONSTANT_EVALUATE = "[";
	public static final String SYMBOL_PREFIX = "prefix";
	public static final String SYMBOL_INFIX = "infix";

	public static class ConstantEvaluatingCompilerState<E> implements IStateProvider<E> {
		private final IValueParser<E> valueParser;
		private final OperatorDictionary<E> operators;
		private final Environment<E> env;
		private final String openingBracket;
		private boolean isFinished;

		public ConstantEvaluatingCompilerState(IValueParser<E> valueParser, OperatorDictionary<E> operators, Environment<E> env, String openingBracket) {
			this.valueParser = valueParser;
			this.operators = operators;
			this.env = env;
			this.openingBracket = openingBracket;
		}

		@Override
		public IPostfixCompilerState<E> createState() {
			return new SimplePostfixCompilerState<E>(new DefaultExecutableListBuilder<E>(valueParser, operators)) {

				@Override
				public Result acceptToken(Token token) {
					if (token.type == TokenType.RIGHT_BRACKET) {
						TokenUtils.checkIsValidBracketPair(openingBracket, token.value);
						isFinished = true;
						return Result.ACCEPTED_AND_FINISHED;
					}
					return super.acceptToken(token);
				}

				@Override
				public IExecutable<E> exit() {
					Preconditions.checkState(isFinished, "Missing closing bracket");
					final IExecutable<E> compiledExpr = super.exit();
					final TopFrame<E> resultFrame = env.executeIsolated(compiledExpr);
					final List<IExecutable<E>> computedValues = Lists.newArrayList();
					for (E value : resultFrame.stack())
						computedValues.add(Value.create(value));
					return new ExecutableList<E>(computedValues);
				}

			};
		}
	}

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

	public static class ParserSwitchTransition<E> implements ISymbolStateTransition<E> {
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

	public static class SwitchingCompilerState<E> implements ICompilerState<E> {

		private final IAstParser<E> parser;

		private final String currentStateSymbol;
		private final String switchStateSymbol;
		private ICompilerState<E> switchState;

		public SwitchingCompilerState(IAstParser<E> parser, String currentStateSymbol, String switchStateSymbol) {
			this.parser = parser;
			this.currentStateSymbol = currentStateSymbol;
			this.switchStateSymbol = switchStateSymbol;
		}

		public SwitchingCompilerState<E> setSwitchState(ICompilerState<E> switchState) {
			this.switchState = switchState;
			return this;
		}

		@Override
		public ISymbolStateTransition<E> getStateForSymbol(final String symbol) {
			if (symbol.equals(switchStateSymbol)) return new ParserSwitchTransition<E>(switchState);
			if (symbol.equals(currentStateSymbol)) return new ParserSwitchTransition<E>(this);

			return new ISymbolStateTransition<E>() {
				@Override
				public ICompilerState<E> getState() {
					return SwitchingCompilerState.this;
				}

				@Override
				public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
					return new SymbolNode<E>(symbol, children);
				}
			};
		}

		@Override
		public IModifierStateTransition<E> getStateForModifier(String modifier) {
			throw new UnsupportedOperationException(modifier);
		}

		@Override
		public IAstParser<E> getParser() {
			return parser;
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
		final SwitchingCompilerState<E> prefixCompilerState = createPrefixCompilerState(operators, exprNodeFactory);
		final SwitchingCompilerState<E> infixCompilerState = createInfixParserState(operators, exprNodeFactory);

		infixCompilerState.setSwitchState(prefixCompilerState);
		prefixCompilerState.setSwitchState(infixCompilerState);

		final Map<ExprType, ICompiler<E>> compilers = Maps.newHashMap();
		compilers.put(ExprType.PREFIX, new WrappedCompiler<E>(prefixTokenizer, createPrefixParser(prefixCompilerState)));
		compilers.put(ExprType.INFIX, new WrappedCompiler<E>(infixTokenizer, createInfixParser(infixCompilerState)));
		compilers.put(ExprType.POSTFIX, new WrappedCompiler<E>(postfixTokenizer, createPostfixParser(valueParser, operators, environment)));
		return new Compilers<E, ExprType>(compilers);
	}

	protected void setupPrefixTokenizer(Tokenizer tokenizer) {}

	protected SwitchingCompilerState<E> createPrefixCompilerState(OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		final IAstParser<E> prefixParser = new PrefixParser<E>(operators, exprNodeFactory);
		return new SwitchingCompilerState<E>(prefixParser, SYMBOL_PREFIX, SYMBOL_INFIX);
	}

	protected ITokenStreamCompiler<E> createPrefixParser(ICompilerState<E> compilerState) {
		return new AstCompiler<E>(compilerState);
	}

	protected void setupInfixTokenizer(Tokenizer tokenizer) {}

	protected SwitchingCompilerState<E> createInfixParserState(OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		final IAstParser<E> infixParser = new InfixParser<E>(operators, exprNodeFactory);
		return new SwitchingCompilerState<E>(infixParser, SYMBOL_INFIX, SYMBOL_PREFIX);
	}

	protected ITokenStreamCompiler<E> createInfixParser(ICompilerState<E> compilerState) {
		return new AstCompiler<E>(compilerState);
	}

	protected void setupPostfixTokenizer(Tokenizer tokenizer) {}

	protected ITokenStreamCompiler<E> createPostfixParser(final IValueParser<E> valueParser, final OperatorDictionary<E> operators, final Environment<E> env) {
		final DefaultPostfixCompiler<E> compiler = new DefaultPostfixCompiler<E>(valueParser, operators);
		return addConstantEvaluatorState(valueParser, operators, env, compiler);
	}

	public static <E> DefaultPostfixCompiler<E> addConstantEvaluatorState(final IValueParser<E> valueParser, final OperatorDictionary<E> operators, final Environment<E> env, final DefaultPostfixCompiler<E> compiler) {
		return compiler.addBracketStateProvider(BRACKET_CONSTANT_EVALUATE, new ConstantEvaluatingCompilerState<E>(valueParser, operators, env, BRACKET_CONSTANT_EVALUATE));
	}

	protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
		return new DefaultExprNodeFactory<E>(valueParser);
	}

}
