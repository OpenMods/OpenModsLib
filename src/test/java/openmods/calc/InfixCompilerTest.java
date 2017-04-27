package openmods.calc;

import openmods.calc.executable.OperatorDictionary;
import openmods.calc.parsing.ast.IAstParser;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.InfixParser;
import openmods.calc.parsing.ast.UnfinishedExpressionException;
import openmods.calc.parsing.ast.UnmatchedBracketsException;
import openmods.calc.parsing.token.Token;
import org.junit.Test;

public class InfixCompilerTest extends AstParserTestUtils {

	public final OperatorDictionary<DummyOperator> operators = new OperatorDictionary<DummyOperator>();
	{
		operators.registerOperator(PLUS);
		operators.registerOperator(UNARY_PLUS);
		operators.registerOperator(MINUS);
		operators.registerOperator(UNARY_MINUS);
		operators.registerOperator(UNARY_NEG);
		operators.registerOperator(MULTIPLY);

		// token for default added only for testing purposes
		operators.registerOperator(DEFAULT).setDefault();
	}

	private final IParserState<TestAstNode> testState = new TestParserState() {
		@Override
		public IAstParser<TestAstNode> getParser() {
			return new InfixParser<TestAstNode, DummyOperator>(operators, EXPR_FACTORY);
		}
	};

	private ParserResultTester given(Token... inputs) {
		return new ParserResultTester(testState, inputs);
	}

	@Test
	public void testSimpleExpr() {
		// 1 + 2
		given(dec("1"), OP_PLUS, dec("2"))
				.expect(operator(PLUS, valueDec("1"), valueDec("2")));

		given(dec("1"), OP_MINUS, dec("2"))
				.expect(operator(MINUS, valueDec("1"), valueDec("2")));
	}

	@Test
	public void testMultipleSamePrecendenceOps() {
		// 1 + 2 - 3
		given(dec("1"), OP_PLUS, dec("2"), OP_MINUS, dec("3"))
				.expect(operator(MINUS, operator(PLUS, valueDec("1"), valueDec("2")), valueDec("3")));
	}

	@Test
	public void testMultipleDifferentPrecendenceOps() {
		// 1 + 2 * 3
		given(dec("1"), OP_PLUS, dec("2"), OP_MULTIPLY, dec("3"))
				.expect(operator(PLUS, valueDec("1"), operator(MULTIPLY, valueDec("2"), valueDec("3"))));

		// 1 * 2 + 3
		given(dec("1"), OP_MULTIPLY, dec("2"), OP_PLUS, dec("3"))
				.expect(operator(PLUS, operator(MULTIPLY, valueDec("1"), valueDec("2")), valueDec("3")));
	}

	@Test
	public void testBrackets() {
		// (1 + 2) * 3
		given(LEFT_BRACKET, dec("1"), OP_PLUS, dec("2"), RIGHT_BRACKET, OP_MULTIPLY, dec("3"))
				.expect(operator(MULTIPLY, brackets(operator(PLUS, valueDec("1"), valueDec("2"))), valueDec("3")));

		// 1 * [2 + 3]
		given(dec("1"), OP_MULTIPLY, leftBracket("["), dec("2"), OP_PLUS, dec("3"), rightBracket("]"))
				.expect(operator(MULTIPLY, valueDec("1"), squareBrackets(operator(PLUS, valueDec("2"), valueDec("3")))));

		// [1]
		given(leftBracket("["), dec("1"), rightBracket("]"))
				.expect(squareBrackets(valueDec("1")));

		// {1}
		given(leftBracket("{"), dec("1"), rightBracket("}"))
				.expect(brackets("{", "}", valueDec("1")));
	}

	@Test
	public void testNestedBrackets() {
		// (1 + (2 - (3)))
		given(LEFT_BRACKET, dec("1"), OP_PLUS, LEFT_BRACKET, dec("2"), OP_MINUS, LEFT_BRACKET, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(brackets(operator(PLUS, valueDec("1"), brackets(operator(MINUS, valueDec("2"), brackets(valueDec("3")))))));

		// ((1))
		given(LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(brackets(brackets(valueDec("1"))));

		// (((1)))
		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(brackets(brackets(brackets(valueDec("1")))));

		// (((1) + 2) - 3)
		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, OP_PLUS, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"), RIGHT_BRACKET)
				.expect(brackets(operator(MINUS, brackets(operator(PLUS, brackets(valueDec("1")), valueDec("2"))), valueDec("3"))));

		// [{(1) + 2} - 3]
		given(leftBracket("["), leftBracket("{"), LEFT_BRACKET, dec("1"), RIGHT_BRACKET, OP_PLUS, dec("2"), rightBracket("}"), OP_MINUS, dec("3"), rightBracket("]"))
				.expect(squareBrackets(operator(MINUS, brackets("{", "}", operator(PLUS, brackets(valueDec("1")), valueDec("2"))), valueDec("3"))));
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
				.expect(operator(PLUS, get("pi"), valueDec("2")));

		// 2 + pi
		given(dec("2"), OP_PLUS, symbol("pi"))
				.expect(operator(PLUS, valueDec("2"), get("pi")));

		// a + b
		given(symbol("a"), OP_PLUS, symbol("b"))
				.expect(operator(PLUS, get("a"), get("b")));

		// sin(a)
		given(symbol("sin"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(call("sin", get("a")));
	}

	@Test
	public void testNullaryFunction() {
		// test()
		given(symbol("test"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(call("test"));

		// p() + e()
		given(symbol("pi"), LEFT_BRACKET, RIGHT_BRACKET, OP_PLUS, symbol("e"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(operator(PLUS, call("pi"), call("e")));

	}

	@Test
	public void testUnaryFunction() {
		// sin(2)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(call("sin", valueDec("2")));

		// sin((2))
		given(symbol("sin"), LEFT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(call("sin", brackets(valueDec("2"))));

		// sin(2 + 3)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), RIGHT_BRACKET)
				.expect(call("sin", operator(PLUS, valueDec("2"), valueDec("3"))));

		// sin((1 + 3))
		given(symbol("sin"), LEFT_BRACKET, LEFT_BRACKET, dec("1"), OP_PLUS, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(call("sin", brackets(operator(PLUS, valueDec("1"), valueDec("3")))));
	}

	@Test
	public void testBinaryFunction() {
		// exp(2, 3)
		given(symbol("exp"), LEFT_BRACKET, dec("2"), COMMA, dec("3"), RIGHT_BRACKET)
				.expect(call("exp", valueDec("2"), valueDec("3")));

		// exp(2 + 3, 3 - 4)
		given(symbol("exp"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), COMMA, dec("4"), OP_MINUS, dec("5"), RIGHT_BRACKET)
				.expect(call("exp", operator(PLUS, valueDec("2"), valueDec("3")), operator(MINUS, valueDec("4"), valueDec("5"))));

		// exp((2), 3)
		given(symbol("exp"), LEFT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, COMMA, dec("3"), RIGHT_BRACKET)
				.expect(call("exp", brackets(valueDec("2")), valueDec("3")));
	}

	@Test
	public void testFunctionSum() {
		// sin(2) + cos(3)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(operator(PLUS, call("sin", valueDec("2")), call("cos", valueDec("3"))));

		// sin(2) + exp(3, 4)
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("exp"), LEFT_BRACKET, dec("3"), COMMA, dec("4"), RIGHT_BRACKET)
				.expect(operator(PLUS, call("sin", valueDec("2")), call("exp", valueDec("3"), valueDec("4"))));
	}

	@Test
	public void testNestedFunctions() {
		// exp(pi, e)
		given(symbol("exp"), LEFT_BRACKET, symbol("pi"), COMMA, symbol("e"), RIGHT_BRACKET)
				.expect(call("exp", get("pi"), get("e")));
		// sin(cos(3))
		given(symbol("sin"), LEFT_BRACKET, symbol("cos"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(call("sin", call("cos", valueDec("3"))));

		// exp(sin(3), 4 + cos(2))
		given(symbol("exp"), LEFT_BRACKET, symbol("sin"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, COMMA, dec("4"), OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(call("exp", call("sin", valueDec("3")), operator(PLUS, valueDec("4"), call("cos", valueDec("2")))));
	}

	@Test
	public void testUnaryOperators() {
		// -2
		given(OP_MINUS, dec("2"))
				.expect(operator(UNARY_MINUS, valueDec("2")));

		// !3
		given(OP_NEG, dec("3"))
				.expect(operator(UNARY_NEG, valueDec("3")));

		// 1 - -2
		given(dec("1"), OP_MINUS, OP_MINUS, dec("2"))
				.expect(operator(MINUS, valueDec("1"), operator(UNARY_MINUS, valueDec("2"))));

		// 1 * -2
		given(dec("1"), OP_MULTIPLY, OP_MINUS, dec("2"))
				.expect(operator(MULTIPLY, valueDec("1"), operator(UNARY_MINUS, valueDec("2"))));

		// 1 * (-2)
		given(dec("1"), OP_MULTIPLY, LEFT_BRACKET, OP_MINUS, dec("2"), RIGHT_BRACKET)
				.expect(operator(MULTIPLY, valueDec("1"), brackets(operator(UNARY_MINUS, valueDec("2")))));

		// (2) - 3
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"))
				.expect(operator(MINUS, brackets(valueDec("2")), valueDec("3")));

		// -(2)
		given(OP_MINUS, LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(operator(UNARY_MINUS, brackets(valueDec("2"))));

		// -(2 - 3)
		given(OP_MINUS, LEFT_BRACKET, dec("2"), OP_MINUS, dec("3"), RIGHT_BRACKET)
				.expect(operator(UNARY_MINUS, brackets(operator(MINUS, valueDec("2"), valueDec("3")))));

		// -pi
		given(OP_MINUS, symbol("pi"))
				.expect(operator(UNARY_MINUS, get("pi")));

		// sin(-2)
		given(symbol("sin"), LEFT_BRACKET, OP_MINUS, dec("2"), RIGHT_BRACKET)
				.expect(call("sin", operator(UNARY_MINUS, valueDec("2"))));

		// -sin(2)
		given(OP_MINUS, symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(operator(UNARY_MINUS, call("sin", valueDec("2"))));
	}

	@Test
	public void testDoubleUnaryOperators() {
		// --2
		given(OP_MINUS, OP_MINUS, dec("2"))
				.expect(operator(UNARY_MINUS, operator(UNARY_MINUS, valueDec("2"))));

		// -+2
		given(OP_MINUS, OP_PLUS, dec("2"))
				.expect(operator(UNARY_MINUS, operator(UNARY_PLUS, valueDec("2"))));

		// -!2
		given(OP_MINUS, OP_NEG, dec("2"))
				.expect(operator(UNARY_MINUS, operator(UNARY_NEG, valueDec("2"))));

		// !-+2
		given(OP_NEG, OP_MINUS, OP_PLUS, dec("2"))
				.expect(operator(UNARY_NEG, operator(UNARY_MINUS, operator(UNARY_PLUS, valueDec("2")))));
	}

	@Test
	public void testDefaultOperator() {
		// 2a == 2 * a
		given(dec("2"), symbol("a"))
				.expect(operator(DEFAULT, valueDec("2"), get("a")))
				.expectSameAs(dec("2"), OP_DEFAULT, symbol("a"));

		// -2a == -2 * a
		given(OP_MINUS, dec("2"), symbol("a"))
				.expect(operator(DEFAULT, operator(UNARY_MINUS, valueDec("2")), get("a")))
				.expectSameAs(OP_MINUS, dec("2"), OP_DEFAULT, symbol("a"));

		// -2a() == -2 * a()
		given(OP_MINUS, dec("2"), symbol("a"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(operator(DEFAULT, operator(UNARY_MINUS, valueDec("2")), call("a")))
				.expectSameAs(OP_MINUS, dec("2"), OP_DEFAULT, symbol("a"), LEFT_BRACKET, RIGHT_BRACKET);

		// 2(a) = 2 * (a)
		given(dec("2"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, valueDec("2"), brackets(get("a"))))
				.expectSameAs(dec("2"), OP_DEFAULT, LEFT_BRACKET, symbol("a"), RIGHT_BRACKET);

		// 3 a + 2 = 3 * a + 2
		given(dec("3"), symbol("a"), OP_PLUS, dec("2"))
				.expect(operator(PLUS, operator(DEFAULT, valueDec("3"), get("a")), valueDec("2")))
				.expectSameAs(dec("3"), OP_DEFAULT, symbol("a"), OP_PLUS, dec("2"));

		// 3 + 2a == 3 + 2 * a
		given(dec("3"), OP_PLUS, dec("2"), symbol("a"))
				.expect(operator(PLUS, valueDec("3"), operator(DEFAULT, valueDec("2"), get("a"))))
				.expectSameAs(dec("3"), OP_PLUS, dec("2"), OP_DEFAULT, symbol("a"));

		// 3a(2) == 3 * a(2)
		given(dec("3"), symbol("a"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, valueDec("3"), call("a", valueDec("2"))))
				.expectSameAs(dec("3"), OP_DEFAULT, symbol("a"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET);

		// 2 3 = 2 * 3 (weird edge condition, but whatever)
		given(dec("2"), dec("3"))
				.expect(operator(DEFAULT, valueDec("2"), valueDec("3")))
				.expectSameAs(dec("2"), OP_DEFAULT, dec("3"));

		// 3(2) == 3 * (2)
		given(dec("3"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, valueDec("3"), brackets(valueDec("2"))))
				.expectSameAs(dec("3"), OP_DEFAULT, LEFT_BRACKET, dec("2"), RIGHT_BRACKET);

		// (2)3 = (2) * 3
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, dec("3"))
				.expect(operator(DEFAULT, brackets(valueDec("2")), valueDec("3")))
				.expectSameAs(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_DEFAULT, dec("3"));

		// (2) i == (2) * i
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, symbol("i"))
				.expect(operator(DEFAULT, brackets(valueDec("2")), get("i")))
				.expectSameAs(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_DEFAULT, symbol("i"));

		// -(2) i == -(2) * i
		given(OP_MINUS, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, symbol("i"))
				.expect(operator(DEFAULT, operator(UNARY_MINUS, brackets(valueDec("2"))), get("i")))
				.expectSameAs(OP_MINUS, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_DEFAULT, symbol("i"));

		// a(2i, 4) == a(2*i, 4)
		given(symbol("a"), LEFT_BRACKET, dec("2"), symbol("i"), COMMA, dec("4"), RIGHT_BRACKET)
				.expect(call("a", operator(DEFAULT, valueDec("2"), get("i")), valueDec("4")))
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("2"), OP_DEFAULT, symbol("i"), COMMA, dec("4"), RIGHT_BRACKET);

		// (2)(3) == (2) * (3)
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, brackets(valueDec("2")), brackets(valueDec("3"))))
				.expectSameAs(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_DEFAULT, LEFT_BRACKET, dec("3"), RIGHT_BRACKET);

		// (2)5(3) == (2) * 5 * (3)
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, dec("5"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, operator(DEFAULT, brackets(valueDec("2")), valueDec("5")), brackets(valueDec("3"))))
				.expectSameAs(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_DEFAULT, dec("5"), OP_DEFAULT, LEFT_BRACKET, dec("3"), RIGHT_BRACKET);

		// (2)a(3) = (2) * a(3)
		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, brackets(valueDec("2")), call("a", valueDec("3"))))
				.expectSameAs(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_DEFAULT, symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET);

		// a(3)2 = a(3) * 2
		given(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, dec("2"))
				.expect(operator(DEFAULT, call("a", valueDec("3")), valueDec("2")))
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, OP_DEFAULT, dec("2"));

		// a(3)j = a(3) * j
		given(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, symbol("j"))
				.expect(operator(DEFAULT, call("a", valueDec("3")), get("j")))
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, OP_DEFAULT, symbol("j"));

		// a(3)(2) = a(3) * (2)
		given(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, call("a", valueDec("3")), brackets(valueDec("2"))))
				.expectSameAs(symbol("a"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, OP_DEFAULT, LEFT_BRACKET, dec("2"), RIGHT_BRACKET);

		// a[3] = a * [3]
		given(symbol("a"), leftBracket("["), dec("3"), rightBracket("]"))
				.expect(operator(DEFAULT, get("a"), squareBrackets(valueDec("3"))))
				.expectSameAs(symbol("a"), OP_DEFAULT, leftBracket("["), dec("3"), rightBracket("]"));

		// a 3 = a * 3
		given(symbol("a"), dec("3"))
				.expect(operator(DEFAULT, get("a"), valueDec("3")))
				.expectSameAs(symbol("a"), OP_DEFAULT, dec("3"));

		// a b = a * b
		given(symbol("a"), symbol("b"))
				.expect(operator(DEFAULT, get("a"), get("b")))
				.expectSameAs(symbol("a"), OP_DEFAULT, symbol("b"));

		// -a b = -a * b
		given(OP_MINUS, symbol("a"), symbol("b"))
				.expect(operator(DEFAULT, operator(UNARY_MINUS, get("a")), get("b")))
				.expectSameAs(OP_MINUS, symbol("a"), OP_DEFAULT, symbol("b"));
	}

	@Test
	public void testValueModifierQuote() {
		// #12
		given(QUOTE_MODIFIER, dec("12")).expect(quoteModifier(quotedToken(dec("12"))));
		// #"abc"
		given(QUOTE_MODIFIER, string("abc")).expect(quoteModifier(quotedToken(string("abc"))));
	}

	@Test
	public void testValueSymbolQuote() {
		// quote(12)
		given(QUOTE_SYMBOL, LEFT_BRACKET, dec("12"), RIGHT_BRACKET).expect(quoteSymbol(quotedToken(dec("12"))));
		// quote("abc")
		given(QUOTE_SYMBOL, LEFT_BRACKET, string("abc"), RIGHT_BRACKET).expect(quoteSymbol(quotedToken(string("abc"))));
	}

	@Test
	public void testBracketSymbolQuote() {
		// quote((12))
		given(QUOTE_SYMBOL, LEFT_BRACKET, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteSymbol(quotedBrackets("(", ")", quotedToken(dec("12")))));
		// quote(("abc"))
		given(QUOTE_SYMBOL, LEFT_BRACKET, LEFT_BRACKET, string("abc"), RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteSymbol(quotedBrackets("(", ")", quotedToken(string("abc")))));
	}

	@Test
	public void testRawValueQuote() {
		// #+
		given(QUOTE_MODIFIER, OP_PLUS).expect(quoteModifier(quotedToken(OP_PLUS)));

		// quote(+)
		given(QUOTE_SYMBOL, LEFT_BRACKET, OP_PLUS, RIGHT_BRACKET).expect(quoteSymbol(quotedToken(OP_PLUS)));

		// #.
		given(QUOTE_MODIFIER, mod(".")).expect(quoteModifier(quotedToken(mod("."))));

		// quote(.)
		given(QUOTE_SYMBOL, LEFT_BRACKET, mod("."), RIGHT_BRACKET).expect(quoteSymbol(quotedToken(mod("."))));
	}

	@Test
	public void testDoubleElementQuote() {
		// quote(1,+,2)
		given(QUOTE_SYMBOL, LEFT_BRACKET, dec("1"), COMMA, OP_PLUS, COMMA, dec("2"), RIGHT_BRACKET)
				.expect(quoteSymbol(quotedToken(dec("1")), quotedToken(OP_PLUS), quotedToken(dec("2"))));
		// #[1,2,3]
		given(QUOTE_MODIFIER, leftBracket("["), dec("1"), COMMA, OP_PLUS, COMMA, dec("2"), rightBracket("]"))
				.expect(quoteModifier(quotedBrackets("[", "]", quotedToken(dec("1")), quotedToken(COMMA), quotedToken(OP_PLUS), quotedToken(COMMA), quotedToken(dec("2")))));
	}

	@Test
	public void testQuoteAndDeafultOpOrder() {
		// #abc(12)
		given(QUOTE_MODIFIER, symbol("abc"), LEFT_BRACKET, dec("12"), RIGHT_BRACKET)
				.expectSameAs(QUOTE_MODIFIER, symbol("abc"), OP_DEFAULT, LEFT_BRACKET, dec("12"), RIGHT_BRACKET)
				.expect(operator(DEFAULT, quoteModifier(quotedToken(symbol("abc"))), brackets(valueDec("12"))));
	}
}
