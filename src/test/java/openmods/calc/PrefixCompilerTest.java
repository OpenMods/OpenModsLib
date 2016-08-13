package openmods.calc;

import java.util.List;
import openmods.calc.parsing.AstCompiler;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.PrefixParser;
import openmods.calc.parsing.Token;
import org.junit.Test;

public class PrefixCompilerTest extends CalcTestUtils {

	public final OperatorDictionary<String> operators = new OperatorDictionary<String>();
	{
		operators.registerBinaryOperator(PLUS);
		operators.registerBinaryOperator(MINUS);
		operators.registerBinaryOperator(ASSIGN);
		operators.registerUnaryOperator(UNARY_NEG);
		operators.registerUnaryOperator(UNARY_MINUS);
	}

	private static final Token CLOSE_LIST = rightBracket("]");
	private static final Token OPEN_LIST = leftBracket("[");

	public static final String SYMBOL_LIST = "list";

	public static class TestBracketNode implements IExprNode<String> {
		private final List<IExprNode<String>> children;

		public TestBracketNode(List<IExprNode<String>> children) {
			this.children = children;
		}

		@Override
		public void flatten(List<IExecutable<String>> output) {
			for (IExprNode<String> child : children)
				child.flatten(output);

			output.add(call(SYMBOL_LIST, children.size()));
		}

		@Override
		public Iterable<IExprNode<String>> getChildren() {
			return children;
		}
	}

	private final ICompilerState<String> testState = new TestCompilerState() {
		@Override
		public IAstParser<String> getParser() {
			return new PrefixParser<String>(operators, new DefaultExprNodeFactory<String>(VALUE_PARSER) {
				@Override
				public IExprNode<String> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<String>> children) {
					return new TestBracketNode(children);
				}
			});
		}
	};

	private CompilerResultTester given(Token... inputs) {
		return new CompilerResultTester(new AstCompiler<String>(testState), inputs);
	}

	@Test
	public void testSingleExpressions() {
		given(dec("12")).expect(c("12"));
		given(string("a")).expect(c("a"));
		given(symbol("a")).expect(get("a"));
	}

	@Test
	public void testSymbolCall() {
		given(LEFT_BRACKET, symbol("a"), RIGHT_BRACKET).expect(call("a", 0));
		given(LEFT_BRACKET, symbol("a"), dec("12"), RIGHT_BRACKET).expect(c("12"), call("a", 1));
		given(LEFT_BRACKET, symbol("a"), dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), call("a", 2));
	}

	@Test
	public void testSymbolAsArg() {
		given(LEFT_BRACKET, symbol("a"), symbol("b"), RIGHT_BRACKET).expect(get("b"), call("a", 1));
		given(LEFT_BRACKET, symbol("a"), dec("12"), symbol("b"), RIGHT_BRACKET).expect(c("12"), get("b"), call("a", 2));
		given(LEFT_BRACKET, symbol("a"), symbol("b"), dec("12"), RIGHT_BRACKET).expect(get("b"), c("12"), call("a", 2));
	}

	@Test
	public void testNestedSymbolCall() {
		given(LEFT_BRACKET, symbol("a"), dec("12"), LEFT_BRACKET, symbol("b"), dec("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(c("12"), c("34"), call("b", 1), call("a", 2));
		given(LEFT_BRACKET, symbol("a"), LEFT_BRACKET, symbol("b"), dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(c("12"), call("b", 1), c("34"), call("a", 2));
	}

	@Test
	public void testSquareBrackets() {
		given(OPEN_LIST, CLOSE_LIST).expect(call(SYMBOL_LIST, 0));
		given(OPEN_LIST, dec("12"), CLOSE_LIST).expect(c("12"), call(SYMBOL_LIST, 1));
		given(OPEN_LIST, dec("12"), dec("34"), CLOSE_LIST).expect(c("12"), c("34"), call(SYMBOL_LIST, 2));
	}

	@Test
	public void testSymbolInSquareBrackets() {
		given(OPEN_LIST, symbol("a"), CLOSE_LIST).expect(get("a"), call(SYMBOL_LIST, 1));
		given(OPEN_LIST, dec("12"), symbol("a"), CLOSE_LIST).expect(c("12"), get("a"), call(SYMBOL_LIST, 2));
		given(OPEN_LIST, symbol("a"), dec("12"), CLOSE_LIST).expect(get("a"), c("12"), call(SYMBOL_LIST, 2));
	}

	@Test
	public void testMixedBrackets() {
		given(LEFT_BRACKET, symbol("print"), OPEN_LIST, dec("12"), CLOSE_LIST, RIGHT_BRACKET).expect(c("12"), call(SYMBOL_LIST, 1), call("print", 1));
		given(OPEN_LIST, LEFT_BRACKET, symbol("print"), dec("12"), RIGHT_BRACKET, CLOSE_LIST).expect(c("12"), call("print", 1), call(SYMBOL_LIST, 1));
	}

	@Test
	public void testCommaWhitespace() {
		given(LEFT_BRACKET, symbol("a"), dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), call("a", 2));
		given(LEFT_BRACKET, symbol("a"), COMMA, dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), call("a", 2));
		given(LEFT_BRACKET, symbol("a"), dec("12"), COMMA, dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), call("a", 2));

		given(OPEN_LIST, symbol("a"), COMMA, dec("12"), COMMA, dec("34"), CLOSE_LIST).expect(get("a"), c("12"), c("34"), call(SYMBOL_LIST, 3));

		given(LEFT_BRACKET, OP_PLUS, dec("12"), COMMA, dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), PLUS);
	}

	@Test
	public void testUnaryOperator() {
		given(LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET).expect(c("12"), UNARY_MINUS);
		given(LEFT_BRACKET, OP_NEG, dec("12"), RIGHT_BRACKET).expect(c("12"), UNARY_NEG);
	}

	@Test
	public void testUnaryOperatorWithNestedArgs() {
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(c("12"), UNARY_MINUS, UNARY_MINUS);
		given(LEFT_BRACKET, OP_MINUS, OPEN_LIST, dec("12"), dec("34"), CLOSE_LIST, RIGHT_BRACKET).expect(c("12"), c("34"), call(SYMBOL_LIST, 2), UNARY_MINUS);
	}

	@Test
	public void testBinaryOperatorWithNestedArgs() {
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(c("12"), UNARY_MINUS, c("34"), MINUS);
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, symbol("a"), dec("34"), RIGHT_BRACKET, OPEN_LIST, dec("56"), CLOSE_LIST, RIGHT_BRACKET).expect(c("34"), call("a", 1), c("56"), call(SYMBOL_LIST, 1), MINUS);
	}

	@Test
	public void testBinaryLeftAssociativeOperator() {
		given(LEFT_BRACKET, OP_MINUS, dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), MINUS);
		given(LEFT_BRACKET, OP_MINUS, dec("12"), dec("34"), dec("56"), RIGHT_BRACKET).expect(c("12"), c("34"), MINUS, c("56"), MINUS);
		given(LEFT_BRACKET, OP_MINUS, dec("12"), dec("34"), dec("56"), dec("78"), RIGHT_BRACKET).expect(c("12"), c("34"), MINUS, c("56"), MINUS, c("78"), MINUS);
	}

	@Test
	public void testBinaryRightAssociativeOperator() {
		given(LEFT_BRACKET, OP_ASSIGN, dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), ASSIGN);
		given(LEFT_BRACKET, OP_ASSIGN, dec("12"), dec("34"), dec("56"), RIGHT_BRACKET).expect(c("12"), c("34"), c("56"), ASSIGN, ASSIGN);
		given(LEFT_BRACKET, OP_ASSIGN, dec("12"), dec("34"), dec("56"), dec("78"), RIGHT_BRACKET).expect(c("12"), c("34"), c("56"), c("78"), ASSIGN, ASSIGN, ASSIGN);
	}

	@Test
	public void testValueModifierQuote() {
		given(QUOTE_MODIFIER, dec("12")).expect(OPEN_ROOT_QUOTE_M, valueMarker("12"), CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, string("abc")).expect(OPEN_ROOT_QUOTE_M, valueMarker("abc"), CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testValueSymbolQuote() {
		given(LEFT_BRACKET, QUOTE_SYMBOL, dec("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, valueMarker("12"), CLOSE_ROOT_QUOTE_S);
		given(LEFT_BRACKET, QUOTE_SYMBOL, string("abc"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, valueMarker("abc"), CLOSE_ROOT_QUOTE_S);
	}

	@Test
	public void testNestedEmptyModifierQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testNestedEmptySymbolQuote() {
		given(LEFT_BRACKET, QUOTE_SYMBOL, LEFT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, OPEN_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE_S);
	}

	@Test
	public void testNestedValueModifierQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), oct("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, valueMarker("12"), valueMarker("34"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testNestedValueSymbolQuote() {
		given(LEFT_BRACKET, QUOTE_SYMBOL, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_S);
		given(LEFT_BRACKET, QUOTE_SYMBOL, LEFT_BRACKET, dec("12"), oct("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, OPEN_QUOTE, valueMarker("12"), valueMarker("34"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_S);
	}

	@Test
	public void testRawValueQuote() {
		given(QUOTE_MODIFIER, symbol("12")).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(symbol("12")), CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, OP_PLUS).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(OP_PLUS), CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, mod(".")).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(mod(".")), CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testNestedRawValueQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, symbol("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, rawValueMarker(symbol("12")), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, LEFT_BRACKET, symbol("12"), OP_PLUS, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, rawValueMarker(symbol("12")), rawValueMarker(OP_PLUS), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, LEFT_BRACKET, symbol("12"), mod("."), OP_PLUS, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, rawValueMarker(symbol("12")), rawValueMarker(mod(".")), rawValueMarker(OP_PLUS), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testDoubleNestedQuote() {
		given(QUOTE_MODIFIER, LEFT_BRACKET, LEFT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, OPEN_QUOTE, CLOSE_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, LEFT_BRACKET, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
		given(QUOTE_MODIFIER, LEFT_BRACKET, dec("0"), LEFT_BRACKET, dec("12"), RIGHT_BRACKET, dec("3"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, valueMarker("0"), OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, valueMarker("3"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M);
	}

	@Test
	public void testModifierQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, dec("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, valueMarker("12"), CLOSE_ROOT_QUOTE_M, call("test", 1));
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, symbol("12"), dec("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, rawValueMarker(symbol("12")), CLOSE_ROOT_QUOTE_M, c("34"), call("test", 2));
	}

	@Test
	public void testSymboQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), LEFT_BRACKET, QUOTE_SYMBOL, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, valueMarker("12"), CLOSE_ROOT_QUOTE_S, call("test", 1));
		given(LEFT_BRACKET, symbol("test"), LEFT_BRACKET, QUOTE_SYMBOL, symbol("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_S, rawValueMarker(symbol("12")), CLOSE_ROOT_QUOTE_S, c("34"), call("test", 2));
	}

	@Test
	public void testNestedQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M, call("test", 1));
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M, c("34"), call("test", 2));
		given(LEFT_BRACKET, symbol("test"), QUOTE_MODIFIER, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, LEFT_BRACKET, symbol("sqrt"), dec("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE_M, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE_M, c("34"), call("sqrt", 1), call("test", 2));
	}
}
