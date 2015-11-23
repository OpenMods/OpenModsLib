package openmods.calc;

import static openmods.calc.TokenUtils.*;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class TokenizerTest {

	private final ExprTokenizerFactory factory = new ExprTokenizerFactory();

	private void verifyTokens(String input, Token... tokens) {
		final Iterable<Token> it = factory.tokenize(input);
		final Token[] result = Iterables.toArray(it, Token.class);
		Assert.assertArrayEquals(tokens, result);
	}

	@Test
	public void testSingleZeros() {
		verifyTokens("0", dec("0")); // special case
		verifyTokens(".0", f(".0"));
		verifyTokens(".0e0", f(".0e0"));
		verifyTokens("0.", f("0."));
		verifyTokens("0.e0", f("0.e0"));
		verifyTokens("0.0e0", f("0.0e0"));
		verifyTokens("0e0", f("0e0"));
		verifyTokens("00.", f("00."));
		verifyTokens("0.0", f("0.0"));
		verifyTokens("00", oct("0"));
		verifyTokens("0x0", hex("0"));
		verifyTokens("0b0", bin("0"));
		verifyTokens("0#0", quoted("0#0"));
		verifyTokens("0#'0'", quoted("0#'0'"));
	}

	@Test
	public void testSingleOnes() {
		verifyTokens("1", dec("1"));
		verifyTokens("1e0", f("1e0"));
		verifyTokens("1.", f("1."));
		verifyTokens("1.e0", f("1.e0"));
		verifyTokens(".1", f(".1"));
		verifyTokens(".1e0", f(".1e0"));
		verifyTokens("1.0", f("1.0"));
		verifyTokens("1.0e0", f("1.0e0"));
		verifyTokens("01", oct("1"));
		verifyTokens("0x1", hex("1"));
		verifyTokens("0b1", bin("1"));
		verifyTokens("1#1", quoted("1#1"));
		verifyTokens("1#'1'", quoted("1#'1'"));
	}

	@Test
	public void testTwoDigits() {
		verifyTokens("12", dec("12"));
		verifyTokens(".12", f(".12"));
		verifyTokens(".12e2", f(".12e2"));
		verifyTokens("12.12", f("12.12"));
		verifyTokens("12.1e2", f("12.1e2"));
		verifyTokens("012", oct("12"));
		verifyTokens("0x12", hex("12"));
		verifyTokens("0b10", bin("10"));
		verifyTokens("12#34", quoted("12#34"));
		verifyTokens("12#'3''4'", quoted("12#'3''4'"));
		verifyTokens("12#'3\"4'", quoted("12#'3\"4'"));
	}

	@Test
	public void testSpecialDigits() {
		verifyTokens("0xDEAD", hex("DEAD"));
		verifyTokens("0xf00d", hex("f00d"));
		verifyTokens("432#12dZsd3", quoted("432#12dZsd3"));
	}

	@Test
	public void testSymbols() {
		verifyTokens("hello", symbol("hello"));
		verifyTokens("f00", symbol("f00"));
		verifyTokens("hi_world", symbol("hi_world"));
		verifyTokens("HelloWorld", symbol("HelloWorld"));
		verifyTokens("$boom_2", symbol("$boom_2"));
		verifyTokens("$1", symbol("$1"));
		verifyTokens("$HELLO", symbol("$HELLO"));
		verifyTokens("_", constant("_")); // !?
		verifyTokens("_C", constant("_C"));
		verifyTokens("HELLO", constant("HELLO"));
		verifyTokens("PI_2", constant("PI_2"));
	}

	@Test
	public void testSingleOperator() {
		factory.addOperator("+");
		verifyTokens("1+2", dec("1"), op("+"), dec("2"));
	}

	@Test
	public void testCommonPrefixOperators() {
		factory.addOperator("+");
		factory.addOperator("++");
		verifyTokens("1++2", dec("1"), op("++"), dec("2"));
		verifyTokens("1+2", dec("1"), op("+"), dec("2"));
	}

	@Test
	public void testTwoOperators() {
		factory.addOperator("+");
		factory.addOperator("-");
		verifyTokens("0x1+0b1-16#4324-$1+3.4",
				hex("1"),
				op("+"),
				bin("1"),
				op("-"),
				quoted("16#4324"),
				op("-"),
				symbol("$1"),
				op("+"),
				f("3.4"));
	}

	@Test
	public void testFullyAlphaOperator() {
		factory.addOperator("not");
		verifyTokens("not here", op("not"), symbol("here"));
	}

	@Test
	public void testFullyAlphaOperatorVsSymbol() {
		factory.addOperator("neg");
		verifyTokens("neg", op("neg"));
		verifyTokens("neg negate", op("neg"), symbol("negate"));
		verifyTokens("negate", symbol("negate"));
	}

	@Test
	public void testPartiallyAlphaOperatorNonAlphaFirst() {
		factory.addOperator("++x");
		verifyTokens("++x", op("++x"));
		verifyTokens("5++x6", dec("5"), op("++x"), dec("6"));
		verifyTokens("hello++xworld", symbol("hello"), op("++x"), symbol("world"));
	}

	@Test
	public void testPartiallyAlphaOperatorAlphaFirst() {
		factory.addOperator("x++");
		verifyTokens("x++", op("x++"));
		verifyTokens("5x++6", dec("5"), op("x++"), dec("6"));
		verifyTokens("hello x++world", symbol("hello"), op("x++"), symbol("world"));
	}

	@Test
	public void testPartiallyAlphaOperatorSamePrefix() {
		factory.addOperator("++a");
		factory.addOperator("++");
		verifyTokens("++a", op("++a"));
		verifyTokens("++abc", op("++a"), symbol("bc"));
		verifyTokens("++ abc", op("++"), symbol("abc"));
	}

	@Test
	public void testWhitespace() {
		factory.addOperator("+");
		verifyTokens(" 1+2", dec("1"), op("+"), dec("2"));
		verifyTokens("1+2 ", dec("1"), op("+"), dec("2"));
		verifyTokens("1 + 2", dec("1"), op("+"), dec("2"));
		verifyTokens("1  +   2", dec("1"), op("+"), dec("2"));
		verifyTokens("\t1\t+\t2\t", dec("1"), op("+"), dec("2"));
	}

	@Test
	public void testBrackets() {
		factory.addOperator("+");
		verifyTokens("()", LEFT_BRACKET, RIGHT_BRACKET);
		verifyTokens("(,)", LEFT_BRACKET, COMMA, RIGHT_BRACKET);
		verifyTokens("(1,2)", LEFT_BRACKET, dec("1"), COMMA, dec("2"), RIGHT_BRACKET);
		verifyTokens(" ( 1 , 2 ) ", LEFT_BRACKET, dec("1"), COMMA, dec("2"), RIGHT_BRACKET);
		verifyTokens("(1+0x2,2)", LEFT_BRACKET, dec("1"), op("+"), hex("2"), COMMA, dec("2"), RIGHT_BRACKET);
	}
}
