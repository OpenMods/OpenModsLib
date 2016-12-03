package openmods.calc;

import openmods.calc.parsing.AstCompiler;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.InfixParser;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.UnfinishedExpressionException;
import openmods.calc.parsing.UnmatchedBracketsException;
import org.junit.Test;

public class InfixCompilerTest extends CalcTestUtils {

	public final OperatorDictionary<String> operators = new OperatorDictionary<String>();
	{
		operators.registerBinaryOperator(PLUS);
		operators.registerUnaryOperator(UNARY_PLUS);
		operators.registerBinaryOperator(MINUS);
		operators.registerUnaryOperator(UNARY_MINUS);
		operators.registerUnaryOperator(UNARY_NEG);
		operators.registerBinaryOperator(MULTIPLY);

		// token for default added only for testing purposes
		operators.registerBinaryOperator(DEFAULT).setDefault();
	}

	private final ICompilerState<String> testState = new TestCompilerState() {
		@Override
		public IAstParser<String> getParser() {
			return new InfixParser<String>(operators, new DefaultExprNodeFactory<String>(VALUE_PARSER));
		}
	};

	private CompilerResultTester given(Token... inputs) {
		return new CompilerResultTester(new AstCompiler<String>(testState), inputs);
	}

	@Test
	public void testSimpleExpr() {
		// 1 + 2
		given(dec("1"), OP_PLUS, dec("2"))
				.expect(c("1"), c("2"), PLUS);

		given(dec("1"), OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), MINUS);
	}

	@Test
	public void testMultipleSamePrecendenceOps() {
		// 1 + 2 - 3
		given(dec("1"), OP_PLUS, dec("2"), OP_MINUS, dec("3"))
				.expect(c("1"), c("2"), PLUS, c("3"), MINUS);
	}

	@Test
	public void testMultipleDifferentPrecendenceOps() {
		// 1 + 2 * 3
		given(dec("1"), OP_PLUS, dec("2"), OP_MULTIPLY, dec("3"))
				.expect(c("1"), c("2"), c("3"), MULTIPLY, PLUS);

		// 1 * 2 + 3
		given(dec("1"), OP_MULTIPLY, dec("2"), OP_PLUS, dec("3"))
				.expect(c("1"), c("2"), MULTIPLY, c("3"), PLUS);
	}

	@Test
	public void testBrackets() {
		// (1 + 2) * 3
		given(LEFT_BRACKET, dec("1"), OP_PLUS, dec("2"), RIGHT_BRACKET, OP_MULTIPLY, dec("3"))
				.expect(c("1"), c("2"), PLUS, c("3"), MULTIPLY);

		// 1 * (2 + 3)
		given(dec("1"), OP_MULTIPLY, LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), c("3"), PLUS, MULTIPLY);

		// [1]
		given(leftBracket("["), dec("1"), rightBracket("]"))
				.expect(c("1"));

		// {1}
		given(leftBracket("{"), dec("1"), rightBracket("}"))
				.expect(c("1"));
	}

	@Test
	public void testNestedBrackets() {
		// (1 + (2 - (3)))
		given(LEFT_BRACKET, dec("1"), OP_PLUS, LEFT_BRACKET, dec("2"), OP_MINUS, LEFT_BRACKET, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("1"), c("2"), c("3"), MINUS, PLUS);

		// ((1))
		given(LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("1"));

		// (((1)))
		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("1"));

		// (((1) + 2) - 3)
		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, OP_PLUS, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), PLUS, c("3"), MINUS);

		// [{(1) + 2} - 3]
		given(leftBracket("["), leftBracket("{"), LEFT_BRACKET, dec("1"), RIGHT_BRACKET, OP_PLUS, dec("2"), rightBracket("}"), OP_MINUS, dec("3"), rightBracket("]"))
				.expect(c("1"), c("2"), PLUS, c("3"), MINUS);
	}

	@Test(expected = UnmatchedBracketsException.class)
	public void testUnmatchedBrackets() {
		given(leftBracket("["), dec("2"), rightBracket("}"));
	}

	@Test(expected = UnmatchedBracketsException.class)
	public void testUnmatchedBracketsOnUnaryFunction() {
		given(symbol("a"), leftBracket("("), dec("2"), rightBracket("}"));
	}

	@Test(expected = UnmatchedBracketsException.class)
	public void testUnmatchedBracketsOnBinaryFunction() {
		given(symbol("b"), leftBracket("("), dec("2"), COMMA, dec("3"), rightBracket("]"));
	}

	@Test(expected = UnfinishedExpressionException.class)
	public void testUnclosedBracket() {
		given(LEFT_BRACKET, dec("2"));
	}

	@Test(expected = UnfinishedExpressionException.class)
	public void testUnclosedBracketWithComma() {
		given(LEFT_BRACKET, dec("2"), COMMA, dec("3"));
	}

	@Test
	public void testSymbolGet() {
		// pi
		given(symbol("pi"))
				.expect(get("pi"));

		// pi + 2
		given(symbol("pi"), OP_PLUS, dec("2"))
				.expect(get("pi"), c("2"), PLUS);

		// 2 + pi
		given(dec("2"), OP_PLUS, symbol("pi"))
				.expect(c("2"), get("pi"), PLUS);

		// a + b
		given(symbol("a"), OP_PLUS, symbol("b"))
				.expect(get("a"), get("b"), PLUS);

		// sin(a)
		given(symbol("sin"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(get("a"), call("sin", 1));
	}

	@Test
	public void testNullaryFunction() {
		// test()
		given(symbol("test"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(call("test", 0));

		// p() + e()
		given(symbol("pi"), LEFT_BRACKET, RIGHT_BRACKET, OP_PLUS, symbol("e"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(call("pi", 0), call("e", 0), PLUS);

	}

	@Test
	public void testUnaryFunction() {
		// sin(2)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), call("sin", 1));

		// sin((2))
		given(symbol("sin"), LEFT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("2"), call("sin", 1));

		// sin(2 + 3)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), PLUS, call("sin", 1));

		// sin((1))
		given(symbol("sin"), LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("1"), call("sin", 1));
	}

	@Test
	public void testBinaryFunction() {
		// exp(2, 3)
		given(symbol("exp"), LEFT_BRACKET, dec("2"), COMMA, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), call("exp", 2));

		// exp(2 + 3, 3 - 4)
		given(symbol("exp"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), COMMA, dec("4"), OP_MINUS, dec("5"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), PLUS, c("4"), c("5"), MINUS, call("exp", 2));

		// exp((2), 3)
		given(symbol("exp"), LEFT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, COMMA, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), call("exp", 2));
	}

	@Test
	public void testFunctionSum() {
		// sin(2) + cos(3)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), call("sin", 1), c("3"), call("cos", 1), PLUS);

		// sin(2) + exp(3, 4)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("exp"), LEFT_BRACKET, dec("3"), COMMA, dec("4"), RIGHT_BRACKET)
				.expect(c("2"), call("sin", 1), c("3"), c("4"), call("exp", 2), PLUS);
	}

	@Test
	public void testNestedFunctions() {
		// exp(pi, e)
		given(symbol("exp"), LEFT_BRACKET, symbol("pi"), COMMA, symbol("e"), RIGHT_BRACKET)
				.expect(get("pi"), get("e"), call("exp", 2));
		// sin(cos(3))
		given(symbol("sin"), LEFT_BRACKET, symbol("cos"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("3"), call("cos", 1), call("sin", 1));

		// exp(sin(3), 4 + cos(2))
		given(symbol("exp"), LEFT_BRACKET, symbol("sin"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, COMMA, dec("4"), OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("3"), call("sin", 1), c("4"), c("2"), call("cos", 1), PLUS, call("exp", 2));
	}

	@Test
	public void testUnaryOperators() {
		// -2
		given(OP_MINUS, dec("2"))
				.expect(c("2"), UNARY_MINUS);

		// !3
		given(OP_NEG, dec("3"))
				.expect(c("3"), UNARY_NEG);

		// 1 - -2
		given(dec("1"), OP_MINUS, OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), UNARY_MINUS, MINUS);

		// 1 * -2
		given(dec("1"), OP_MULTIPLY, OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), UNARY_MINUS, MULTIPLY);

		// 1 * (-2)
		given(dec("1"), OP_MULTIPLY, LEFT_BRACKET, OP_MINUS, dec("2"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), UNARY_MINUS, MULTIPLY);

		// (2) - 3
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"))
				.expect(c("2"), c("3"), MINUS);

		// -(2)
		given(OP_MINUS, LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), UNARY_MINUS);

		// -(2 - 3)
		given(OP_MINUS, LEFT_BRACKET, dec("2"), OP_MINUS, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), MINUS, UNARY_MINUS);

		// -pi
		given(OP_MINUS, symbol("pi"))
				.expect(get("pi"), UNARY_MINUS);

		// sin(-2)
		given(symbol("sin"), LEFT_BRACKET, OP_MINUS, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), UNARY_MINUS, call("sin", 1));

		// -sin(2)
		given(OP_MINUS, symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), call("sin", 1), UNARY_MINUS);
	}

	@Test
	public void testDoubleUnaryOperators() {
		// --2
		given(OP_MINUS, OP_MINUS, dec("2"))
				.expect(c("2"), UNARY_MINUS, UNARY_MINUS);

		// -+2
		given(OP_MINUS, OP_PLUS, dec("2"))
				.expect(c("2"), UNARY_PLUS, UNARY_MINUS);

		// -!2
		given(OP_MINUS, OP_NEG, dec("2"))
				.expect(c("2"), UNARY_NEG, UNARY_MINUS);

		// !-+2
		given(OP_NEG, OP_MINUS, OP_PLUS, dec("2"))
				.expect(c("2"), UNARY_PLUS, UNARY_MINUS, UNARY_NEG);
	}

	@Test
	public void testDefaultOperator() {
		// 2a == 2 * a
		given(dec("2"), symbol("a"))
				.expect(c("2"), get("a"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, symbol("a"));

		// -2a == -2 * a
		given(OP_MINUS, dec("2"), symbol("a"))
				.expect(c("2"), UNARY_MINUS, get("a"), DEFAULT)
				.expectSameAs(OP_MINUS, dec("2"), OP_DEFAULT, symbol("a"));

		// -2a() == -2 * a()
		given(OP_MINUS, dec("2"), symbol("a"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(c("2"), UNARY_MINUS, call("a", 0), DEFAULT)
				.expectSameAs(OP_MINUS, dec("2"), OP_DEFAULT, symbol("a"), LEFT_BRACKET, RIGHT_BRACKET);

		// 2(a) = 2 * (a)
		given(dec("2"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(c("2"), get("a"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, symbol("a"));

		// 3 a + 2 = 3 * a + 2
		given(dec("3"), symbol("a"), OP_PLUS, dec("2"))
				.expect(c("3"), get("a"), DEFAULT, c("2"), PLUS);

		// 3 + 2a == 3 + 2 * a
		given(dec("3"), OP_PLUS, dec("2"), symbol("a"))
				.expect(c("3"), c("2"), get("a"), DEFAULT, PLUS)
				.expectSameAs(dec("3"), OP_PLUS, dec("2"), OP_DEFAULT, symbol("a"));

		// 3a(2) == 3 * a(2)
		given(dec("3"), symbol("a"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("3"), c("2"), call("a", 1), DEFAULT)
				.expectSameAs(dec("3"), OP_DEFAULT, symbol("a"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET);

		// 2 3 = 2 * 3 (weird edge condition, but whatever)
		given(dec("2"), dec("3"))
				.expect(c("2"), c("3"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, dec("3"));

		// 3(2) == 3 * 2
		given(dec("3"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("3"), c("2"), DEFAULT)
				.expectSameAs(dec("3"), OP_DEFAULT, dec("2"));

		// (2)3 = 2 * 3
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, dec("3"))
				.expect(c("2"), c("3"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, dec("3"));

		// (2) i == 2 * i
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, symbol("i"))
				.expect(c("2"), get("i"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, symbol("i"));

		// -(2) i == -2 * i
		given(OP_MINUS, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, symbol("i"))
				.expect(c("2"), UNARY_MINUS, get("i"), DEFAULT)
				.expectSameAs(OP_MINUS, dec("2"), OP_DEFAULT, symbol("i"));

		// a(2i, 4) == a(2*i, 4)
		given(symbol("a"), LEFT_BRACKET, dec("2"), symbol("i"), COMMA, dec("4"), RIGHT_BRACKET)
				.expect(c("2"), get("i"), DEFAULT, c("4"), call("a", 2))
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("2"), OP_DEFAULT, symbol("i"), COMMA, dec("4"), RIGHT_BRACKET);

		// (2)(3) == 2 * 3
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, dec("3"));

		// (2)5(3) == 2 * 5 * 3
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, dec("5"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("5"), DEFAULT, c("3"), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, dec("5"), OP_DEFAULT, dec("3"));

		// (2)a(3) = 2 * a(3)
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), call("a", 1), DEFAULT)
				.expectSameAs(dec("2"), OP_DEFAULT, symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET);

		// a(3)2 = a(3) * 2
		given(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, dec("2"))
				.expect(c("3"), call("a", 1), c("2"), DEFAULT)
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, OP_DEFAULT, dec("2"));

		// a(3)j = a(3) * j
		given(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, symbol("j"))
				.expect(c("3"), call("a", 1), get("j"), DEFAULT)
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, OP_DEFAULT, symbol("j"));

		// a(3)(2) = a(3) * 2
		given(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("3"), call("a", 1), c("2"), DEFAULT)
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, OP_DEFAULT, dec("2"));

		// a[3] = a * [3]
		given(symbol("a"), leftBracket("["), dec("3"), rightBracket("]"))
				.expect(get("a"), c("3"), DEFAULT)
				.expectSameAs(symbol("a"), OP_DEFAULT, dec("3"));

		// a 3 = a * 3
		given(symbol("a"), dec("3"))
				.expect(get("a"), c("3"), DEFAULT)
				.expectSameAs(symbol("a"), OP_DEFAULT, dec("3"));

		// a b = a * b
		given(symbol("a"), symbol("b"))
				.expect(get("a"), get("b"), DEFAULT)
				.expectSameAs(symbol("a"), OP_DEFAULT, symbol("b"));

		// -a b = -a * b
		given(OP_MINUS, symbol("a"), symbol("b"))
				.expect(get("a"), UNARY_MINUS, get("b"), DEFAULT)
				.expectSameAs(OP_MINUS, symbol("a"), OP_DEFAULT, symbol("b"));
	}

	@Test
	public void testValueModifierQuote() {
		// #12
		given(QUOTE_MODIFIER, dec("12")).expect(OPEN_ROOT_QUOTE_M, valueMarker("12"), CLOSE_ROOT_QUOTE_M);
		// #"abc"
		given(QUOTE_MODIFIER, string("abc")).expect(OPEN_ROOT_QUOTE_M, valueMarker("abc"), CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testValueSymbolQuote() {
		// quote(12)
		given(QUOTE_SYMBOL, LEFT_BRACKET, dec("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, valueMarker("12"), CLOSE_ROOT_QUOTE_S);
		// quote("abc")
		given(QUOTE_SYMBOL, LEFT_BRACKET, string("abc"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, valueMarker("abc"), CLOSE_ROOT_QUOTE_S);
	}

	@Test
	public void testRawValueQuote() {
		// #abc
		given(QUOTE_MODIFIER, symbol("abc")).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(symbol("abc")), CLOSE_ROOT_QUOTE_M);
		// #+
		given(QUOTE_MODIFIER, OP_PLUS).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(OP_PLUS), CLOSE_ROOT_QUOTE_M);
		// #.
		given(QUOTE_MODIFIER, mod(".")).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(mod(".")), CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testQuoteAndDeafultOpOrder() {
		// #abc(12)
		given(QUOTE_MODIFIER, symbol("abc"), LEFT_BRACKET, dec("12"), RIGHT_BRACKET)
				.expectSameAs(QUOTE_MODIFIER, symbol("abc"), OP_DEFAULT, LEFT_BRACKET, dec("12"), RIGHT_BRACKET)
				.expect(OPEN_ROOT_QUOTE_M, rawValueMarker(symbol("abc")), CLOSE_ROOT_QUOTE_M, c("12"), DEFAULT);
	}
}
