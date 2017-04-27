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
import openmods.calc.ExprType;
import openmods.calc.Frame;
import openmods.calc.ICompilerMapFactory;
import openmods.calc.executable.ExecutableList;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.Operator;
import openmods.calc.executable.Value;
import openmods.calc.parsing.ast.IAstParser;
import openmods.calc.parsing.ast.IModifierStateTransition;
import openmods.calc.parsing.ast.INodeFactory;
import openmods.calc.parsing.ast.IOperatorDictionary;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.ISymbolCallStateTransition;
import openmods.calc.parsing.ast.InfixParser;
import openmods.calc.parsing.ast.MappedParserState;
import openmods.calc.parsing.ast.PrefixParser;
import openmods.calc.parsing.node.DefaultExprNodeFactory;
import openmods.calc.parsing.node.DummyNode;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.SymbolCallNode;
import openmods.calc.parsing.postfix.BracketPostfixParserStateBase;
import openmods.calc.parsing.postfix.IExecutableListBuilder;
import openmods.calc.parsing.postfix.IPostfixParserState;
import openmods.calc.parsing.postfix.MappedPostfixParser;
import openmods.calc.parsing.postfix.MappedPostfixParser.IStateProvider;
import openmods.calc.parsing.postfix.PostfixParser;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.Tokenizer;

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

	public static class ParserSwitchTransition<E> implements ISymbolCallStateTransition<IExprNode<E>> {
		private IParserState<IExprNode<E>> switchState;

		public ParserSwitchTransition(IParserState<IExprNode<E>> switchState) {
			this.switchState = switchState;
		}

		@Override
		public IParserState<IExprNode<E>> getState() {
			return switchState;
		}

		@Override
		public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
			Preconditions.checkState(children.size() == 1, "Expected one node, got %s", children);
			return new DummyNode<E>(children.get(0));
		}
	}

	@Override
	public Compilers<E, ExprType> create(E nullValue, IValueParser<E> valueParser, IOperatorDictionary<Operator<E>> operators, Environment<E> environment) {
		final Tokenizer prefixTokenizer = new Tokenizer();

		final Tokenizer infixTokenizer = new Tokenizer();

		final Tokenizer postfixTokenizer = new Tokenizer();

		for (String operator : operators.allOperatorIds()) {
			prefixTokenizer.addOperator(operator);
			infixTokenizer.addOperator(operator);
			postfixTokenizer.addOperator(operator);
		}

		setupPrefixTokenizer(prefixTokenizer);
		setupInfixTokenizer(infixTokenizer);
		setupPostfixTokenizer(postfixTokenizer);

		final INodeFactory<IExprNode<E>, Operator<E>> exprNodeFactory = createExprNodeFactory(valueParser);
		final MappedParserState<IExprNode<E>> prefixCompilerState = createPrefixCompilerState(operators, exprNodeFactory);
		final MappedParserState<IExprNode<E>> infixCompilerState = createInfixCompilerState(operators, exprNodeFactory);

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

	protected MappedParserState<IExprNode<E>> createCompilerState(final IAstParser<IExprNode<E>> parser) {
		return new MappedParserState<IExprNode<E>>(parser) {
			@Override
			protected IExprNode<E> createDefaultSymbolNode(String symbol, List<IExprNode<E>> children) {
				return new SymbolCallNode<E>(symbol, children);
			}

			@Override
			protected IModifierStateTransition<IExprNode<E>> createDefaultModifierStateTransition(String modifier) {
				throw new UnsupportedOperationException(modifier);
			}
		};
	}

	protected MappedParserState<IExprNode<E>> createPrefixCompilerState(IOperatorDictionary<Operator<E>> operators, INodeFactory<IExprNode<E>, Operator<E>> exprNodeFactory) {
		final IAstParser<IExprNode<E>> prefixParser = new PrefixParser<IExprNode<E>, Operator<E>>(operators, exprNodeFactory);
		return createCompilerState(prefixParser);
	}

	protected ITokenStreamCompiler<E> createPrefixParser(IParserState<IExprNode<E>> compilerState) {
		return new AstCompiler<E>(compilerState);
	}

	protected void setupInfixTokenizer(Tokenizer tokenizer) {}

	protected MappedParserState<IExprNode<E>> createInfixCompilerState(IOperatorDictionary<Operator<E>> operators, INodeFactory<IExprNode<E>, Operator<E>> exprNodeFactory) {
		final IAstParser<IExprNode<E>> infixParser = new InfixParser<IExprNode<E>, Operator<E>>(operators, exprNodeFactory);
		return createCompilerState(infixParser);
	}

	protected ITokenStreamCompiler<E> createInfixParser(IParserState<IExprNode<E>> compilerState) {
		return new AstCompiler<E>(compilerState);
	}

	protected void setupPostfixTokenizer(Tokenizer tokenizer) {
		tokenizer.addModifier(MODIFIER_SYMBOL_GET);
	}

	protected ITokenStreamCompiler<E> createPostfixParser(final IValueParser<E> valueParser, final IOperatorDictionary<Operator<E>> operators, final Environment<E> env) {
		final MappedPostfixParser<IExecutable<E>> compiler = new MappedPostfixParser<IExecutable<E>>() {
			@Override
			protected IExecutableListBuilder<IExecutable<E>> createListBuilder() {
				return new DefaultExecutableListBuilder<E>(valueParser, operators);
			}
		};
		final PostfixParser<IExecutable<E>> parser = addSymbolGetState(addConstantEvaluatorState(valueParser, operators, env, compiler));
		return new PostfixCompiler<E>(parser);
	}

	public static <E> MappedPostfixParser<IExecutable<E>> addConstantEvaluatorState(IValueParser<E> valueParser, IOperatorDictionary<Operator<E>> operators, Environment<E> env, MappedPostfixParser<IExecutable<E>> compiler) {
		return compiler.addBracketStateProvider(BRACKET_CONSTANT_EVALUATE, createConstantEvaluatorStateProvider(valueParser, operators, env, BRACKET_CONSTANT_EVALUATE));
	}

	public static <E> IStateProvider<IExecutable<E>> createConstantEvaluatorStateProvider(final IValueParser<E> valueParser, final IOperatorDictionary<Operator<E>> operators, final Environment<E> env, final String openingBracket) {
		return new IStateProvider<IExecutable<E>>() {
			@Override
			public IPostfixParserState<IExecutable<E>> createState() {
				final IExecutableListBuilder<IExecutable<E>> listBuilder = new DefaultExecutableListBuilder<E>(valueParser, operators);

				class ConstantEvaluatorState extends BracketPostfixParserStateBase<IExecutable<E>> {
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

	public static <E> MappedPostfixParser<IExecutable<E>> addSymbolGetState(MappedPostfixParser<IExecutable<E>> compiler) {
		return compiler.addModifierStateProvider(MODIFIER_SYMBOL_GET, new IStateProvider<IExecutable<E>>() {
			@Override
			public IPostfixParserState<IExecutable<E>> createState() {
				return new SymbolGetPostfixCompilerState<E>();
			}
		});
	}

	protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
		return new DefaultExprNodeFactory<E>(valueParser);
	}

	protected void configureCompilerStateCommon(MappedParserState<IExprNode<E>> compilerState, Environment<E> environment) {}
}
