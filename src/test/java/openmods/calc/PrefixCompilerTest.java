package openmods.calc;

import openmods.calc.executable.OperatorDictionary;
import openmods.calc.parsing.ast.IAstParser;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.PrefixParser;
import openmods.calc.parsing.token.Token;
import org.junit.Test;

public class PrefixCompilerTest extends AstParserTestUtils {

	public final OperatorDictionary<DummyOperator> operators = new OperatorDictionary<DummyOperator>();
	{
		operators.registerOperator(PLUS);
		operators.registerOperator(MINUS);
		operators.registerOperator(ASSIGN);
		operators.registerOperator(UNARY_NEG);
		operators.registerOperator(UNARY_MINUS);
		operators.registerDefaultOperator(DEFAULT);
	}

	private static final Token CLOSE_LIST = rightBracket("]");
	private static final Token OPEN_LIST = leftBracket("[");

	private final IParserState<TestAstNode> testState = new TestParserState() {
		@Override
		public IAstParser<TestAstNode> getParser() {
			return new PrefixParser<TestAstNode, DummyOperator>(operators, EXPR_FACTORY);
		}
	};

	private ParserResultTester given(Token... inputs) {
		return new ParserResultTester(testState, inputs);
	}

	@Test
	public void testSingleExpressions() {
		given(dec("12")).expect(value(dec("12")));
		given(string("a")).expect(value(string("a")));
		given(symbol("a")).expect(get("a"));
	}

	@Test
	public void testSymbolCall() {
		given(LEFT_BRACKET, symbol("a"), RIGHT_BRACKET).expect(call("a"));
		given(LEFT_BRACKET, symbol("a"), dec("12"), RIGHT_BRACKET).expect(call("a", valueDec("12")));
		given(LEFT_BRACKET, symbol("a"), dec("12"), dec("34"), RIGHT_BRACKET).expect(call("a", valueDec("12"), valueDec("34")));
	}

	@Test
	public void testSymbolAsArg() {
		given(LEFT_BRACKET, symbol("a"), symbol("b"), RIGHT_BRACKET).expect(call("a", get("b")));
		given(LEFT_BRACKET, symbol("a"), dec("12"), symbol("b"), RIGHT_BRACKET).expect(call("a", valueDec("12"), get("b")));
		given(LEFT_BRACKET, symbol("a"), symbol("b"), dec("12"), RIGHT_BRACKET).expect(call("a", get("b"), valueDec("12")));
	}

	@Test
	public void testNestedSymbolCall() {
		given(LEFT_BRACKET, symbol("a"), dec("12"), LEFT_BRACKET, symbol("b"), dec("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(call("a", valueDec("12"), call("b", valueDec("34"))));
		given(LEFT_BRACKET, symbol("a"), LEFT_BRACKET, symbol("b"), dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(call("a", call("b", valueDec("12")), valueDec("34")));
	}

	@Test
	public void testSquareBrackets() {
		given(OPEN_LIST, CLOSE_LIST).expect(brackets("[", "]"));
		given(OPEN_LIST, dec("12"), CLOSE_LIST).expect(squareBrackets(valueDec("12")));
		given(OPEN_LIST, dec("12"), dec("34"), CLOSE_LIST).expect(squareBrackets(valueDec("12"), valueDec("34")));
	}

	@Test
	public void testSymbolInSquareBrackets() {
		given(OPEN_LIST, symbol("a"), CLOSE_LIST).expect(squareBrackets(get("a")));
		given(OPEN_LIST, dec("12"), symbol("a"), CLOSE_LIST).expect(squareBrackets(valueDec("12"), get("a")));
		given(OPEN_LIST, symbol("a"), dec("12"), CLOSE_LIST).expect(squareBrackets(get("a"), valueDec("12")));
	}

	@Test
	public void testMixedBrackets() {
		given(LEFT_BRACKET, symbol("print"), OPEN_LIST, dec("12"), CLOSE_LIST, RIGHT_BRACKET).expect(call("print", squareBrackets(valueDec("12"))));
		given(OPEN_LIST, LEFT_BRACKET, symbol("print"), dec("12"), RIGHT_BRACKET, CLOSE_LIST).expect(squareBrackets(call("print", valueDec("12"))));
	}

	@Test
	public void testCommaWhitespace() {
		given(LEFT_BRACKET, symbol("a"), dec("12"), dec("34"), RIGHT_BRACKET).expect(call("a", valueDec("12"), valueDec("34")));
		given(LEFT_BRACKET, symbol("a"), COMMA, dec("12"), dec("34"), RIGHT_BRACKET).expect(call("a", valueDec("12"), valueDec("34")));
		given(LEFT_BRACKET, symbol("a"), dec("12"), COMMA, dec("34"), RIGHT_BRACKET).expect(call("a", valueDec("12"), valueDec("34")));

		given(OPEN_LIST, symbol("a"), COMMA, dec("12"), COMMA, dec("34"), CLOSE_LIST).expect(squareBrackets(get("a"), valueDec("12"), valueDec("34")));

		given(LEFT_BRACKET, OP_PLUS, dec("12"), COMMA, dec("34"), RIGHT_BRACKET).expect(operator(PLUS, valueDec("12"), valueDec("34")));
	}

	@Test
	public void testUnaryOperator() {
		given(LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET).expect(operator(UNARY_MINUS, valueDec("12")));
		given(LEFT_BRACKET, OP_NEG, dec("12"), RIGHT_BRACKET).expect(operator(UNARY_NEG, valueDec("12")));
	}

	@Test
	public void testUnaryOperatorWithNestedArgs() {
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(operator(UNARY_MINUS, operator(UNARY_MINUS, valueDec("12"))));
		given(LEFT_BRACKET, OP_MINUS, OPEN_LIST, dec("12"), dec("34"), CLOSE_LIST, RIGHT_BRACKET).expect(operator(UNARY_MINUS, squareBrackets(valueDec("12"), valueDec("34"))));
	}

	@Test
	public void testBinaryOperatorWithNestedArgs() {
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(operator(MINUS, operator(UNARY_MINUS, valueDec("12")), valueDec("34")));
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, symbol("a"), dec("34"), RIGHT_BRACKET, OPEN_LIST, dec("56"), CLOSE_LIST, RIGHT_BRACKET).expect(operator(MINUS, call("a", valueDec("34")), squareBrackets(valueDec("56"))));
	}

	@Test
	public void testBinaryLeftAssociativeOperator() {
		given(LEFT_BRACKET, OP_MINUS, dec("12"), dec("34"), RIGHT_BRACKET).expect(operator(MINUS, valueDec("12"), valueDec("34")));
		given(LEFT_BRACKET, OP_MINUS, dec("12"), dec("34"), dec("56"), RIGHT_BRACKET).expect(operator(MINUS, operator(MINUS, valueDec("12"), valueDec("34")), valueDec("56")));
		given(LEFT_BRACKET, OP_MINUS, dec("12"), dec("34"), dec("56"), dec("78"), RIGHT_BRACKET).expect(operator(MINUS, operator(MINUS, operator(MINUS, valueDec("12"), valueDec("34")), valueDec("56")), valueDec("78")));
	}

	@Test
	public void testBinaryRightAssociativeOperator() {
		given(LEFT_BRACKET, OP_ASSIGN, dec("12"), dec("34"), RIGHT_BRACKET).expect(operator(ASSIGN, valueDec("12"), valueDec("34")));
		given(LEFT_BRACKET, OP_ASSIGN, dec("12"), dec("34"), dec("56"), RIGHT_BRACKET).expect(operator(ASSIGN, valueDec("12"), operator(ASSIGN, valueDec("34"), valueDec("56"))));
		given(LEFT_BRACKET, OP_ASSIGN, dec("12"), dec("34"), dec("56"), dec("78"), RIGHT_BRACKET).expect(operator(ASSIGN, valueDec("12"), operator(ASSIGN, valueDec("34"), operator(ASSIGN, valueDec("56"), valueDec("78")))));
	}

	@Test
	public void testValueModifierQuote() {
		given(QUOTE_MODIFIER, dec("12")).expect(quoteModifier(quotedToken(dec("12"))));
		given(QUOTE_MODIFIER, string("abc")).expect(quoteModifier(quotedToken(string("abc"))));
	}

	@Test
	public void testValueSymbolQuote() {
		given(LEFT_BRACKET, QUOTE_SYMBOL, dec("12"), RIGHT_BRACKET).expect(quoteSymbol(quotedToken(dec("12"))));
		given(LEFT_BRACKET, QUOTE_SYMBOL, string("abc"), RIGHT_BRACKET).expect(quoteSymbol(quotedToken(string("abc"))));
	}

	@Test
	public void testNestedEmptyModifierQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")")));
	}

	@Test
	public void testNestedEmptySymbolQuote() {
		given(LEFT_BRACKET, QUOTE_SYMBOL, LEFT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteSymbol(quotedBrackets("(", ")")));
	}

	@Test
	public void testNestedValueModifierQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedToken(dec("12")))));
		given(QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), oct("34"), RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedToken(dec("12")), quotedToken(oct("34")))));
	}

	@Test
	public void testNestedValueSymbolQuote() {
		given(LEFT_BRACKET, QUOTE_SYMBOL, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteSymbol(quotedBrackets("(", ")", quotedToken(dec("12")))));
		given(LEFT_BRACKET, QUOTE_SYMBOL, LEFT_BRACKET, dec("12"), oct("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteSymbol(quotedBrackets("(", ")", quotedToken(dec("12")), quotedToken(oct("34")))));
	}

	@Test
	public void testRawValueQuote() {
		given(QUOTE_MODIFIER, symbol("12")).expect(quoteModifier(quotedToken(symbol("12"))));
		given(QUOTE_MODIFIER, OP_PLUS).expect(quoteModifier(quotedToken(OP_PLUS)));
		given(QUOTE_MODIFIER, mod(".")).expect(quoteModifier(quotedToken(mod("."))));
	}

	@Test
	public void testNestedRawValueQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, symbol("12"), RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedToken(symbol("12")))));
		given(QUOTE_MODIFIER, LEFT_BRACKET, symbol("12"), OP_PLUS, RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedToken(symbol("12")), quotedToken(OP_PLUS))));
		given(QUOTE_MODIFIER, LEFT_BRACKET, symbol("12"), mod("."), OP_PLUS, RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedToken(symbol("12")), quotedToken(mod(".")), quotedToken(OP_PLUS))));
	}

	@Test
	public void testDoubleNestedQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, LEFT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedBrackets("(", ")"))));
		given(QUOTE_MODIFIER, LEFT_BRACKET, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedBrackets("(", ")", quotedToken(dec("12"))))));
		given(QUOTE_MODIFIER, LEFT_BRACKET, dec("0"), LEFT_BRACKET, dec("12"), RIGHT_BRACKET, dec("3"), RIGHT_BRACKET).expect(quoteModifier(quotedBrackets("(", ")", quotedToken(dec("0")), quotedBrackets("(", ")", quotedToken(dec("12"))), quotedToken(dec("3")))));
	}

	@Test
	public void testModifierQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, dec("12"), RIGHT_BRACKET).expect(call("test", quoteModifier(quotedToken(dec("12")))));
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, symbol("12"), dec("34"), RIGHT_BRACKET).expect(call("test", quoteModifier(quotedToken(symbol("12"))), valueDec("34")));
	}

	@Test
	public void testSymbolQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), LEFT_BRACKET, QUOTE_SYMBOL, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(call("test", quoteSymbol(quotedToken(dec("12")))));
		given(LEFT_BRACKET, symbol("test"), LEFT_BRACKET, QUOTE_SYMBOL, symbol("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(call("test", quoteSymbol(quotedToken(symbol("12"))), valueDec("34")));
	}

	@Test
	public void testNestedQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(call("test", quoteModifier(quotedBrackets("(", ")", quotedToken(dec("12"))))));
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(call("test", quoteModifier(quotedBrackets("(", ")", quotedToken(dec("12")))), valueDec("34")));
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, LEFT_BRACKET, symbol("sqrt"), dec("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(call("test", quoteModifier(quotedBrackets("(", ")", quotedToken(dec("12")))), call("sqrt", valueDec("34"))));
	}

	@Test
	public void testExpressionOnFirstPosition() {
		given(LEFT_BRACKET, LEFT_BRACKET, OP_PLUS, dec("12"), dec("34"), RIGHT_BRACKET, dec("56"), dec("78"), RIGHT_BRACKET).expect(operator(DEFAULT, operator(PLUS, valueDec("12"), valueDec("34")), brackets(valueDec("56"), valueDec("78"))));
		given(LEFT_BRACKET, LEFT_BRACKET, symbol("test"), RIGHT_BRACKET, dec("12"), dec("34"), RIGHT_BRACKET).expect(operator(DEFAULT, call("test"), brackets(valueDec("12"), valueDec("34"))));
	}

	@Test
	public void testModifierOnFirstPosition() {
		given(LEFT_BRACKET, QUOTE_MODIFIER, LEFT_BRACKET, symbol("test"), RIGHT_BRACKET, dec("12"), dec("34"), RIGHT_BRACKET).expect(operator(DEFAULT, quoteModifier(quotedBrackets("(", ")", quotedToken(symbol("test")))), brackets(valueDec("12"), valueDec("34"))));
	}

	@Test
	public void testValueOnFirstPosition() {
		// I'm not sure if it's good idea, but it's side-effect of default op. Without this there wouldn't be 'apply' synctatic sugar
		given(LEFT_BRACKET, dec("5"), symbol("I"), RIGHT_BRACKET).expect(operator(DEFAULT, valueDec("5"), brackets(get("I"))));
	}
}
