package openmods.calc;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Arrays;
import java.util.List;
import openmods.calc.BinaryOperator.Associativity;
import openmods.calc.Calculator.ExprType;
import openmods.calc.parsing.ContainerNode;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.DummyNode;
import openmods.calc.parsing.EmptyExprNodeFactory;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompiler;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IModifierExprNodeFactory;
import openmods.calc.parsing.ISymbolExprNodeFactory;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.QuotedParser;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import openmods.calc.parsing.TokenUtils;
import org.junit.Assert;

public class CalcTestUtils {

	public static class ValueParserHelper<E> {
		public final IValueParser<E> parser;

		public ValueParserHelper(IValueParser<E> parser) {
			this.parser = parser;
		}

		private E parse(TokenType type, String value) {
			return parser.parseToken(new Token(type, value));
		}

		public E bin(String value) {
			return parse(TokenType.BIN_NUMBER, value);
		}

		public void testBin(E expected, String input) {
			Assert.assertEquals(expected, bin(input));
		}

		public E oct(String value) {
			return parse(TokenType.OCT_NUMBER, value);
		}

		public void testOct(E expected, String input) {
			Assert.assertEquals(expected, oct(input));
		}

		public E dec(String value) {
			return parse(TokenType.DEC_NUMBER, value);
		}

		public void testDec(E expected, String input) {
			Assert.assertEquals(expected, dec(input));
		}

		public E hex(String value) {
			return parse(TokenType.HEX_NUMBER, value);
		}

		public void testHex(E expected, String input) {
			Assert.assertEquals(expected, hex(input));
		}

		public E quoted(String value) {
			return parse(TokenType.QUOTED_NUMBER, value);
		}

		public void testQuoted(E expected, String input) {
			Assert.assertEquals(expected, quoted(input));
		}
	}

	public static Token t(TokenType type, String value) {
		return new Token(type, value);
	}

	public static Token dec(String value) {
		return t(TokenType.DEC_NUMBER, value);
	}

	public static Token oct(String value) {
		return t(TokenType.OCT_NUMBER, value);
	}

	public static Token hex(String value) {
		return t(TokenType.HEX_NUMBER, value);
	}

	public static Token bin(String value) {
		return t(TokenType.BIN_NUMBER, value);
	}

	public static Token quoted(String value) {
		return t(TokenType.QUOTED_NUMBER, value);
	}

	public static Token string(String value) {
		return t(TokenType.STRING, value);
	}

	public static Token symbol(String value) {
		return t(TokenType.SYMBOL, value);
	}

	public static Token symbol_args(String value) {
		return t(TokenType.SYMBOL_WITH_ARGS, value);
	}

	public static Token op(String value) {
		return t(TokenType.OPERATOR, value);
	}

	public static Token mod(String value) {
		return t(TokenType.MODIFIER, value);
	}

	public static Token leftBracket(String value) {
		return t(TokenType.LEFT_BRACKET, value);
	}

	public static Token rightBracket(String value) {
		return t(TokenType.RIGHT_BRACKET, value);
	}

	public static final Token COMMA = t(TokenType.SEPARATOR, ",");
	public static final Token RIGHT_BRACKET = rightBracket(")");
	public static final Token LEFT_BRACKET = leftBracket("(");

	public static final Token QUOTE_MODIFIER = mod(TokenUtils.MODIFIER_QUOTE);

	public static final Token QUOTE_SYMBOL = symbol(TokenUtils.SYMBOL_QUOTE);

	public static class DummyBinaryOperator<E> extends BinaryOperator<E> {

		public DummyBinaryOperator(int precendence, String id) {
			super(id, precendence);
		}

		public DummyBinaryOperator(int precendence, String id, Associativity associativity) {
			super(id, precendence, associativity);
		}

		@Override
		public E execute(E left, E right) {
			return null;
		}
	}

	public static class DummyUnaryOperator<E> extends UnaryOperator<E> {
		public DummyUnaryOperator(String id) {
			super(id);
		}

		@Override
		public E execute(E value) {
			return null;
		}
	}

	public static final BinaryOperator<String> PLUS = new DummyBinaryOperator<String>(1, "+");

	public static final Token OP_PLUS = op("+");

	public static final UnaryOperator<String> UNARY_PLUS = new DummyUnaryOperator<String>("+");

	public static final BinaryOperator<String> MINUS = new DummyBinaryOperator<String>(1, "-");

	public static final Token OP_MINUS = op("-");

	public static final UnaryOperator<String> UNARY_MINUS = new DummyUnaryOperator<String>("-");

	public static final UnaryOperator<String> UNARY_NEG = new DummyUnaryOperator<String>("!");

	public static final Token OP_NEG = op("!");

	public static final BinaryOperator<String> MULTIPLY = new DummyBinaryOperator<String>(2, "*");

	public static final Token OP_MULTIPLY = op("*");

	public static final BinaryOperator<String> ASSIGN = new DummyBinaryOperator<String>(1, "=", Associativity.RIGHT);

	public static final Token OP_ASSIGN = op("=");

	public static IExecutable<String> c(String value) {
		return new Value<String>(value);
	}

	public static SymbolReference<String> s(String value) {
		return new SymbolReference<String>(value);
	}

	public static SymbolReference<String> s(String value, int args) {
		return new SymbolReference<String>(value, args, 1);
	}

	public static SymbolReference<String> s(String value, int args, int rets) {
		return new SymbolReference<String>(value, args, rets);
	}

	public static class MarkerExecutable<E> implements IExecutable<E> {

		public String tag;

		public MarkerExecutable(String tag) {
			this.tag = Strings.nullToEmpty(tag);
		}

		@Override
		public void execute(ICalculatorFrame<E> frame) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String serialize() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return tag.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			return obj instanceof MarkerExecutable
					&& ((MarkerExecutable<?>)obj).tag.equals(this.tag);
		}

		@Override
		public String toString() {
			return "!!!" + tag;
		}

	}

	public static IExecutable<String> marker(String tag) {
		return new MarkerExecutable<String>(tag);
	}

	public static final IValueParser<String> VALUE_PARSER = new IValueParser<String>() {
		@Override
		public String parseToken(Token token) {
			return token.value;
		}
	};

	public static class CalcCheck<E> {
		private final Calculator<E> sut;

		private final IExecutable<E> expr;

		public CalcCheck(Calculator<E> sut, IExecutable<E> expr) {
			this.expr = expr;
			this.sut = sut;
		}

		public CalcCheck<E> expectResult(E value) {
			Assert.assertEquals(value, sut.executeAndPop(expr));
			return this;
		}

		public CalcCheck<E> expectEmptyStack() {
			Assert.assertTrue(Lists.newArrayList(sut.getStack()).isEmpty());
			return this;
		}

		public CalcCheck<E> expectStack(E... values) {
			Assert.assertEquals(Arrays.asList(values), Lists.newArrayList(sut.getStack()));
			return this;
		}

		public CalcCheck<E> execute() {
			sut.execute(expr);
			return this;
		}

		public static <E> CalcCheck<E> create(Calculator<E> sut, String value, ExprType exprType) {
			final IExecutable<E> expr = sut.compile(exprType, value);
			return new CalcCheck<E>(sut, expr);
		}
	}

	public static PeekingIterator<Token> tokenIterator(Token... inputs) {
		return Iterators.peekingIterator(Iterators.forArray(inputs));
	}

	public static class CompilerResultTester {
		private final List<IExecutable<String>> actual;
		private final ICompiler<String> compiler;

		public CompilerResultTester(ICompiler<String> compiler, Token... inputs) {
			this.compiler = compiler;
			final IExecutable<String> result = compiler.compile(tokenIterator(inputs));
			Assert.assertTrue(result instanceof ExecutableList);

			this.actual = ((ExecutableList<String>)result).getCommands();
		}

		public CompilerResultTester expectSameAs(Token... inputs) {
			final IExecutable<?> result = compiler.compile(tokenIterator(inputs));
			Assert.assertTrue(result instanceof ExecutableList);
			Assert.assertEquals(((ExecutableList<?>)result).getCommands(), actual);
			return this;
		}

		public CompilerResultTester expect(IExecutable<?>... expected) {
			Assert.assertEquals(Arrays.asList(expected), actual);
			return this;
		}
	}

	public static final IExecutable<String> CLOSE_QUOTE = rightBracketMarker(")");
	public static final IExecutable<String> OPEN_QUOTE = leftBracketMarker("(");
	public static final IExecutable<String> OPEN_ROOT_QUOTE_M = marker("<<" + TokenUtils.MODIFIER_QUOTE);
	public static final IExecutable<String> CLOSE_ROOT_QUOTE_M = marker(TokenUtils.MODIFIER_QUOTE + ">>");

	public static final IExecutable<String> OPEN_ROOT_QUOTE_S = marker("((" + TokenUtils.SYMBOL_QUOTE);
	public static final IExecutable<String> CLOSE_ROOT_QUOTE_S = marker(TokenUtils.SYMBOL_QUOTE + "))");

	public static IExecutable<String> leftBracketMarker(String value) {
		return marker("<" + value);
	}

	public static IExecutable<String> rightBracketMarker(String value) {
		return marker(value + ">");
	}

	public static IExecutable<String> valueMarker(String value) {
		return marker("value:" + value);
	}

	public static IExecutable<String> rawValueMarker(Token token) {
		return rawValueMarker(token.type, token.value);
	}

	public static IExecutable<String> rawValueMarker(TokenType type, String value) {
		return marker("raw:" + type + ":" + value);
	}

	public static class MarkerNode implements IExprNode<String> {
		private final IExecutable<String> value;

		public MarkerNode(IExecutable<String> value) {
			this.value = value;
		}

		@Override
		public void flatten(List<IExecutable<String>> output) {
			output.add(value);
		}

		@Override
		public Iterable<IExprNode<String>> getChildren() {
			return ImmutableList.of();
		}

	}

	public static class QuoteNodeTestFactory extends EmptyExprNodeFactory<String> {
		@Override
		public IAstParser<String> getParser() {
			return new QuotedParser<String>(VALUE_PARSER, this);
		}

		@Override
		public IExprNode<String> createValueNode(String value) {
			return new MarkerNode(valueMarker(value));
		}

		@Override
		public IExprNode<String> createRawValueNode(Token token) {
			return new MarkerNode(rawValueMarker(token));
		}

		@Override
		public IExprNode<String> createBracketNode(final String openingBracket, final String closingBracket, final List<IExprNode<String>> children) {
			return new IExprNode<String>() {

				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(leftBracketMarker(openingBracket));
					for (IExprNode<String> child : children)
						child.flatten(output);
					output.add(rightBracketMarker(closingBracket));
				}

				@Override
				public Iterable<IExprNode<String>> getChildren() {
					return children;
				}
			};
		}
	}

	public static class ModifierQuoteNodeTestFactory extends QuoteNodeTestFactory implements IModifierExprNodeFactory<String> {
		private final String modifier;

		public ModifierQuoteNodeTestFactory(String modifier) {
			this.modifier = modifier;
		}

		@Override
		public IExprNode<String> createRootModifierNode(IExprNode<String> child) {
			return new DummyNode<String>(child) {
				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(marker("<<" + modifier));
					super.flatten(output);
					output.add(marker(modifier + ">>"));
				}
			};
		}
	}

	public static class SymbolQuoteNodeTestFactory extends QuoteNodeTestFactory implements ISymbolExprNodeFactory<String> {
		private final String symbol;

		public SymbolQuoteNodeTestFactory(String symbol) {
			this.symbol = symbol;
		}

		@Override
		public IExprNode<String> createRootSymbolNode(List<IExprNode<String>> children) {
			return new ContainerNode<String>(children) {
				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(marker("((" + symbol));
					for (IExprNode<String> child : args)
						child.flatten(output);
					output.add(marker(symbol + "))"));
				}

			};
		}
	}

	public abstract static class TestExprNodeFactory extends DefaultExprNodeFactory<String> {

		@Override
		public IModifierExprNodeFactory<String> createModifierExprNodeFactory(String modifier) {
			if (modifier.equals(QUOTE_MODIFIER.value)) return new ModifierQuoteNodeTestFactory(modifier);
			return super.createModifierExprNodeFactory(modifier);
		}

		@Override
		public ISymbolExprNodeFactory<String> createSymbolExprNodeFactory(String symbol) {
			if (symbol.equals(QUOTE_SYMBOL.value)) return new SymbolQuoteNodeTestFactory(symbol);
			return super.createSymbolExprNodeFactory(symbol);
		}
	}
}
