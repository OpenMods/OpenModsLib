package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Collection;
import java.util.Iterator;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.Tokenizer;
import org.junit.Assert;
import org.junit.Test;

public class TokenizerTest extends CalcTestUtils {

	private final Tokenizer factory = new Tokenizer();

	private void verifyTokens(String input, Token... tokens) {
		final PeekingIterator<Token> it = factory.tokenize(input);
		Collection<Token> collection = Lists.newArrayList(it);
		Token[] array = new Token[collection.size()];
		Assert.assertArrayEquals(tokens, collection.toArray(array));
	}

	private void expectFail(String input) {
		try {
			final Iterator<Token> it = factory.tokenize(input);
			final String result = Joiner.on(' ').join(it); // should fail while iterating
			Assert.fail(result);
		} catch (Exception e) {
			// NO-OP
		}
	}

	@Test
	public void testBinary() {
		verifyTokens("0b0", bin("0"));
		verifyTokens("0b1", bin("1"));

		verifyTokens("0b0.0", bin("0.0"));
		verifyTokens("0b1.0", bin("1.0"));

		verifyTokens("0b01", bin("01"));

		verifyTokens("0b1_01", bin("1_01"));
		verifyTokens("0b1__01", bin("1__01"));
		verifyTokens("0b0_11_00", bin("0_11_00"));
		verifyTokens("0b1.1_0", bin("1.1_0"));
		verifyTokens("0b1.1__0", bin("1.1__0"));
		verifyTokens("0b101.00_11", bin("101.00_11"));
		verifyTokens("0b1_1.1_1", bin("1_1.1_1"));

		verifyTokens("0b10_", bin("10"), symbol("_"));
		verifyTokens("0b10.01_", bin("10.01"), symbol("_"));

		// TODO: this is confusing, redo?
		verifyTokens("0b_11", dec("0"), symbol("b_11"));
		verifyTokens("0_b11", dec("0"), symbol("_b11"));
	}

	@Test
	public void testOctal() {
		verifyTokens("00", oct("0"));
		verifyTokens("01", oct("1"));

		verifyTokens("00.0", oct("0.0"));
		verifyTokens("01.0", oct("1.0"));

		verifyTokens("0123", oct("123"));

		verifyTokens("01_23", oct("1_23"));
		verifyTokens("01__23", oct("1__23"));
		verifyTokens("01_23_45", oct("1_23_45"));
		verifyTokens("0123.2_3", oct("123.2_3"));
		verifyTokens("0123.2__3", oct("123.2__3"));
		verifyTokens("0123.23_45", oct("123.23_45"));
		verifyTokens("01_3.2_3", oct("1_3.2_3"));

		verifyTokens("012_", oct("12"), symbol("_"));
		verifyTokens("012.32_", oct("12.32"), symbol("_"));

		verifyTokens("0_123", oct("_123"));
	}

	@Test
	public void testDecimal() {
		verifyTokens("0", dec("0"));
		verifyTokens("1", dec("1"));

		verifyTokens("0.0", dec("0.0"));
		verifyTokens("1.0", dec("1.0"));

		verifyTokens("123", dec("123"));

		verifyTokens("1_23", dec("1_23"));
		verifyTokens("1__23", dec("1__23"));
		verifyTokens("1_23_45", dec("1_23_45"));
		verifyTokens("123.2_3", dec("123.2_3"));
		verifyTokens("123.2__3", dec("123.2__3"));
		verifyTokens("123.23_45", dec("123.23_45"));
		verifyTokens("1_3.2_3", dec("1_3.2_3"));

		verifyTokens("12_", dec("12"), symbol("_"));
		expectFail("_12.32");
		expectFail("12_.32");
		expectFail("12._32");
		verifyTokens("12.32_", dec("12.32"), symbol("_"));
	}

	@Test
	public void testHexadecimal() {
		verifyTokens("0x0", hex("0"));
		verifyTokens("0x0.0", hex("0.0"));

		verifyTokens("0x1", hex("1"));
		verifyTokens("0x1.0", hex("1.0"));

		verifyTokens("0x123", hex("123"));
		verifyTokens("0xABC", hex("ABC"));

		verifyTokens("0xDEAD", hex("DEAD"));
		verifyTokens("0xf00d", hex("f00d"));

		verifyTokens("0x1_B3", hex("1_B3"));
		verifyTokens("0x1__B3", hex("1__B3"));
		verifyTokens("0x1_2A_45", hex("1_2A_45"));
		verifyTokens("0x1B3.A_3", hex("1B3.A_3"));
		verifyTokens("0x1B3.A__3", hex("1B3.A__3"));
		verifyTokens("0x123.2C_C5", hex("123.2C_C5"));
		verifyTokens("0x1_A.2_B", hex("1_A.2_B"));

		verifyTokens("0x12_", hex("12"), symbol("_"));
		verifyTokens("0x12.32_", hex("12.32"), symbol("_"));

		// TODO: this is confusing, redo?
		verifyTokens("0x_12", dec("0"), symbol("x_12"));
		verifyTokens("0_x12", dec("0"), symbol("_x12"));
	}

	@Test
	public void testQuoted() {
		verifyTokens("0#0", quoted("0#0"));
		verifyTokens("0#'0'", quoted("0#'0'"));
		verifyTokens("0#0.0", quoted("0#0.0"));

		verifyTokens("1#1", quoted("1#1"));
		verifyTokens("1#'1'", quoted("1#'1'"));
		verifyTokens("1#1.0", quoted("1#1.0"));

		verifyTokens("12#34", quoted("12#34"));
		verifyTokens("12#'3''4'", quoted("12#'3''4'"));
		verifyTokens("12#'3\"4'", quoted("12#'3\"4'"));

		verifyTokens("432#12dZsd3", quoted("432#12dZsd3"));

		verifyTokens("13#1_B3", quoted("13#1_B3"));
		verifyTokens("13#1__B3", quoted("13#1__B3"));
		verifyTokens("13#1_2A_45", quoted("13#1_2A_45"));
		verifyTokens("13#1B3.A_3", quoted("13#1B3.A_3"));
		verifyTokens("13#1B3.A__3", quoted("13#1B3.A__3"));
		verifyTokens("13#123.2C_C5", quoted("13#123.2C_C5"));
		verifyTokens("14#1_A.2_B", quoted("14#1_A.2_B"));

		verifyTokens("14#12_", quoted("14#12"), symbol("_"));
		verifyTokens("14#12.32_", quoted("14#12.32"), symbol("_"));
		expectFail("_14#12");
		expectFail("14_#12");
	}

	@Test
	public void testStrings() {
		verifyTokens("''", string(""));
		verifyTokens("'abc'", string("abc"));

		verifyTokens("\"\"", string(""));
		verifyTokens("\"abc\"", string("abc"));

		verifyTokens("'a''b'", string("a"), string("b"));
		verifyTokens("'a'\"b\"", string("a"), string("b"));
		verifyTokens("'a' 'b'", string("a"), string("b"));
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
		verifyTokens("_", symbol("_"));
		verifyTokens("_C", symbol("_C"));
		verifyTokens("_123", symbol("_123"));
		verifyTokens("HELLO", symbol("HELLO"));
		verifyTokens("PI_2", symbol("PI_2"));
	}

	@Test
	public void testSymbolsWithArgs() {
		verifyTokens("hello$2", symbol_args("hello$2"));
		verifyTokens("hello$2,2", symbol_args("hello$2,2"));
		verifyTokens("hello$,2", symbol_args("hello$,2"));
		verifyTokens("hello$2,", symbol_args("hello$2,"));
		verifyTokens("hello$,", symbol_args("hello$,")); // weird case, but kept for simplicity
		verifyTokens("hello$12,345", symbol_args("hello$12,345"));
		verifyTokens("$$2", symbol_args("$$2"));
		verifyTokens("$1$3", symbol_args("$1$3"));
		verifyTokens("$ans$3,4", symbol_args("$ans$3,4"));

		factory.addOperator("+");
		verifyTokens("hello + hello$2", symbol("hello"), op("+"), symbol_args("hello$2"));
		verifyTokens("hello$3 + hello$2", symbol_args("hello$3"), op("+"), symbol_args("hello$2"));
		verifyTokens("$ans$3,4+6", symbol_args("$ans$3,4"), op("+"), dec("6"));
	}

	@Test
	public void testArgsSymbol() {
		factory.addOperator("@");
		factory.addOperator("@@");
		verifyTokens("hello$2", symbol_args("hello$2"));
		verifyTokens("@2", op("@"), dec("2"));
		verifyTokens("@@2", op("@@"), dec("2"));
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
	public void testPosfixSymbols() {
		verifyTokens("1i", dec("1"), symbol("i"));
		verifyTokens("0x1i", hex("1"), symbol("i"));
		verifyTokens("16#4324 i", quoted("16#4324"), symbol("i"));
	}

	@Test
	public void testSymbolsAndStrings() {
		verifyTokens("'aaaa'bbbb", string("aaaa"), symbol("bbbb"));
		verifyTokens("bbbb'aaaa'", symbol("bbbb"), string("aaaa"));
	}

	@Test
	public void testTwoOperators() {
		factory.addOperator("+");
		factory.addOperator("-");
		verifyTokens("'abc'-0x1+0b1-16#4324-$1+3.4",
				string("abc"),
				op("-"),
				hex("1"),
				op("+"),
				bin("1"),
				op("-"),
				quoted("16#4324"),
				op("-"),
				symbol("$1"),
				op("+"),
				dec("3.4"));
	}

	@Test
	public void testModifiers() {
		factory.addModifier("+");
		factory.addModifier("-");
		verifyTokens("a+b-c", symbol("a"), mod("+"), symbol("b"), mod("-"), symbol("c"));
	}

	@Test
	public void testFullyAlphaOperator() {
		factory.addOperator("not");
		verifyTokens("not here", op("not"), symbol("here"));
	}

	@Test
	public void testFullyAlphaModifier() {
		factory.addModifier("not");
		verifyTokens("not here", mod("not"), symbol("here"));
	}

	@Test
	public void testFullyAlphaOperatorVsSymbol() {
		factory.addOperator("neg");
		verifyTokens("neg", op("neg"));
		verifyTokens("neg negate", op("neg"), symbol("negate"));
		verifyTokens("negate", symbol("negate"));
	}

	@Test
	public void testFullyAlphaModifierVsSymbol() {
		factory.addModifier("neg");
		verifyTokens("neg", mod("neg"));
		verifyTokens("neg negate", mod("neg"), symbol("negate"));
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
	public void testTwoOperatorsSamePrefix() {
		factory.addOperator("++a");
		factory.addOperator("++");
		verifyTokens("++a", op("++a"));
		verifyTokens("++abc", op("++a"), symbol("bc"));
		verifyTokens("++ abc", op("++"), symbol("abc"));
	}

	@Test
	public void testTwoModifiersSamePrefix() {
		factory.addModifier("++a");
		factory.addModifier("++");
		verifyTokens("++a", mod("++a"));
		verifyTokens("++abc", mod("++a"), symbol("bc"));
		verifyTokens("++ abc", mod("++"), symbol("abc"));
	}

	@Test
	public void testModifierOverOperator() {
		factory.addOperator("++");
		factory.addModifier("++");
		verifyTokens("++", mod("++"));
	}

	@Test
	public void testModifierOverOperatorPartialMatch() {
		factory.addOperator("+");
		factory.addModifier("++");
		verifyTokens("++", mod("++"));
		verifyTokens("+ +", op("+"), op("+"));
		verifyTokens("+++", mod("++"), op("+"));
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
		verifyTokens("[]", leftBracket("["), rightBracket("]"));
		verifyTokens("{}", leftBracket("{"), rightBracket("}"));
		verifyTokens("([{}])", leftBracket("("), leftBracket("["), leftBracket("{"), rightBracket("}"), rightBracket("]"), rightBracket(")"));
		verifyTokens("(,)", LEFT_BRACKET, COMMA, RIGHT_BRACKET);
		verifyTokens("(1,2)", LEFT_BRACKET, dec("1"), COMMA, dec("2"), RIGHT_BRACKET);
		verifyTokens(" ( 1 , 2 ) ", LEFT_BRACKET, dec("1"), COMMA, dec("2"), RIGHT_BRACKET);
		verifyTokens("(1+0x2,2)", LEFT_BRACKET, dec("1"), op("+"), hex("2"), COMMA, dec("2"), RIGHT_BRACKET);
	}
}
