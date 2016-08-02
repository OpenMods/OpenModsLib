package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import java.util.Map;
import openmods.calc.Calculator.ICompiler;
import openmods.calc.parsing.AstCompiler;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.DummyNode;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.ICompilerState.ISymbolStateTransition;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.ITokenStreamCompiler;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.InfixParser;
import openmods.calc.parsing.PostfixCompiler;
import openmods.calc.parsing.PrefixParser;
import openmods.calc.parsing.SymbolNode;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.Tokenizer;

public abstract class BasicCalculatorFactory<E, C extends Calculator<E, ExprType>> implements ICalculatorFactory<E, ExprType, C> {

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
	public C create(E nullValue, IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		final Tokenizer tokenizer = new Tokenizer();

		final Tokenizer extendedTokenizer = new Tokenizer();

		for (String operator : operators.allOperators()) {
			tokenizer.addOperator(operator);
			extendedTokenizer.addOperator(operator);
		}

		setupExtendedTokenizer(extendedTokenizer);

		final IExprNodeFactory<E> exprNodeFactory = createExprNodeFactory(valueParser);
		final SwitchingCompilerState<E> prefixCompilerState = createPrefixCompilerState(operators, exprNodeFactory);
		final SwitchingCompilerState<E> infixCompilerState = createInfixParserState(operators, exprNodeFactory);

		infixCompilerState.setSwitchState(prefixCompilerState);
		prefixCompilerState.setSwitchState(infixCompilerState);

		final Map<ExprType, ICompiler<E>> compilers = Maps.newHashMap();
		compilers.put(ExprType.PREFIX, new WrappedCompiler<E>(extendedTokenizer, new AstCompiler<E>(prefixCompilerState)));
		compilers.put(ExprType.INFIX, new WrappedCompiler<E>(extendedTokenizer, new AstCompiler<E>(infixCompilerState)));
		compilers.put(ExprType.POSTFIX, new WrappedCompiler<E>(tokenizer, new PostfixCompiler<E>(valueParser, operators)));
		return createCalculator(nullValue, compilers);
	}

	protected SwitchingCompilerState<E> createInfixParserState(OperatorDictionary<E> operators, final IExprNodeFactory<E> exprNodeFactory) {
		final IAstParser<E> infixParser = new InfixParser<E>(operators, exprNodeFactory);
		return new SwitchingCompilerState<E>(infixParser, "infix", "prefix");
	}

	protected SwitchingCompilerState<E> createPrefixCompilerState(OperatorDictionary<E> operators, final IExprNodeFactory<E> exprNodeFactory) {
		final IAstParser<E> prefixParser = new PrefixParser<E>(operators, exprNodeFactory);
		return new SwitchingCompilerState<E>(prefixParser, "prefix", "infix");
	}

	protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
		return new DefaultExprNodeFactory<E>(valueParser);
	}

	protected void setupExtendedTokenizer(final Tokenizer extendedTokenizer) {}

	protected abstract C createCalculator(E nullValue, Map<ExprType, ICompiler<E>> compilers);
}
