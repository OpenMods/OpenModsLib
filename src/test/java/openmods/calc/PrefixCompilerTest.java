package openmods.calc;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.DummyNode;
import openmods.calc.parsing.EmptyExprNodeFactory;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.PrefixCompiler;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import org.junit.Assert;
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

	private static final String SYMBOL_LIST = "list";

	private static class TestBracketNode implements IExprNode<String> {
		private final List<IExprNode<String>> children;

		public TestBracketNode(List<IExprNode<String>> children) {
			this.children = children;
		}

		@Override
		public void flatten(List<IExecutable<String>> output) {
			for (IExprNode<String> child : children)
				child.flatten(output);

			output.add(s(SYMBOL_LIST, children.size()));
		}

		@Override
		public Iterable<IExprNode<String>> getChildren() {
			return children;
		}
	}

	private static final Token CLOSE_LIST = rightBracket("]");
	private static final Token OPEN_LIST = leftBracket("[");

	private static final IExecutable<String> CLOSE_QUOTE = rightBracketMarker(")");
	private static final IExecutable<String> OPEN_QUOTE = leftBracketMarker("(");
	private static final IExecutable<String> OPEN_ROOT_QUOTE = marker("<<" + PrefixCompiler.MODIFIER_QUOTE);
	private static final IExecutable<String> CLOSE_ROOT_QUOTE = marker(PrefixCompiler.MODIFIER_QUOTE + ">>");

	private static IExecutable<String> leftBracketMarker(String value) {
		return marker("<" + value);
	}

	private static IExecutable<String> rightBracketMarker(String value) {
		return marker(value + ">");
	}

	private static IExecutable<String> valueMarker(String value) {
		return marker("value:" + value);
	}

	private static IExecutable<String> rawValueMarker(Token token) {
		return rawValueMarker(token.type, token.value);
	}

	private static IExecutable<String> rawValueMarker(TokenType type, String value) {
		return marker("raw:" + type + ":" + value);
	}

	private static class MarkerNode implements IExprNode<String> {
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

	private static class QuoteNodeTestFactory extends EmptyExprNodeFactory<String> {
		@Override
		public IExprNode<String> createValueNode(String value) {
			return new MarkerNode(valueMarker(value));
		}

		@Override
		public IExprNode<String> createRawValueNode(Token token) {
			return new MarkerNode(rawValueMarker(token));
		}

		@Override
		public IExprNode<String> createModifierNode(final String modifier, IExprNode<String> child) {
			return new DummyNode<String>(child) {
				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(marker("<<" + modifier));
					super.flatten(output);
					output.add(marker(modifier + ">>"));
				}

			};
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

	public final IExprNodeFactory<String> nodeFactory = new DefaultExprNodeFactory<String>() {
		@Override
		public IExprNode<String> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<String>> children) {
			return new TestBracketNode(children);
		}

		@Override
		public IExprNodeFactory<String> getExprNodeFactoryForModifier(String modifier) {
			if (modifier.equals(QUOTE.value)) return new QuoteNodeTestFactory();
			return super.getExprNodeFactoryForModifier(modifier);
		}

	};

	private class ResultTester {
		private final PrefixCompiler<?> compiler = new PrefixCompiler<String>(VALUE_PARSER, operators, nodeFactory);

		private final List<?> actual;

		public ResultTester(Token... inputs) {
			final IExecutable<?> result = compiler.compile(Arrays.asList(inputs));
			Assert.assertTrue(result instanceof ExecutableList);

			this.actual = ((ExecutableList<?>)result).getCommands();
		}

		void expect(IExecutable<?>... expected) {
			Assert.assertEquals(Arrays.asList(expected), actual);
		}
	}

	private ResultTester given(Token... inputs) {
		return new ResultTester(inputs);
	}

	@Test
	public void testSingleExpressions() {
		given(dec("12")).expect(c("12"));
		given(string("a")).expect(c("a"));
		given(symbol("a")).expect(s("a", 0));
	}

	@Test
	public void testSymbolCall() {
		given(LEFT_BRACKET, symbol("a"), RIGHT_BRACKET).expect(s("a", 0));
		given(LEFT_BRACKET, symbol("a"), dec("12"), RIGHT_BRACKET).expect(c("12"), s("a", 1));
		given(LEFT_BRACKET, symbol("a"), dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), s("a", 2));
	}

	@Test
	public void testSymbolAsArg() {
		given(LEFT_BRACKET, symbol("a"), symbol("b"), RIGHT_BRACKET).expect(s("b", 0), s("a", 1));
		given(LEFT_BRACKET, symbol("a"), dec("12"), symbol("b"), RIGHT_BRACKET).expect(c("12"), s("b", 0), s("a", 2));
		given(LEFT_BRACKET, symbol("a"), symbol("b"), dec("12"), RIGHT_BRACKET).expect(s("b", 0), c("12"), s("a", 2));
	}

	@Test
	public void testNestedSymbolCall() {
		given(LEFT_BRACKET, symbol("a"), dec("12"), LEFT_BRACKET, symbol("b"), dec("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(c("12"), c("34"), s("b", 1), s("a", 2));
		given(LEFT_BRACKET, symbol("a"), LEFT_BRACKET, symbol("b"), dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(c("12"), s("b", 1), c("34"), s("a", 2));
	}

	@Test
	public void testSquareBrackets() {
		given(OPEN_LIST, CLOSE_LIST).expect(s(SYMBOL_LIST, 0));
		given(OPEN_LIST, dec("12"), CLOSE_LIST).expect(c("12"), s(SYMBOL_LIST, 1));
		given(OPEN_LIST, dec("12"), dec("34"), CLOSE_LIST).expect(c("12"), c("34"), s(SYMBOL_LIST, 2));
	}

	@Test
	public void testSymbolInSquareBrackets() {
		given(OPEN_LIST, symbol("a"), CLOSE_LIST).expect(s("a", 0), s(SYMBOL_LIST, 1));
		given(OPEN_LIST, dec("12"), symbol("a"), CLOSE_LIST).expect(c("12"), s("a", 0), s(SYMBOL_LIST, 2));
		given(OPEN_LIST, symbol("a"), dec("12"), CLOSE_LIST).expect(s("a", 0), c("12"), s(SYMBOL_LIST, 2));
	}

	@Test
	public void testMixedBrackets() {
		given(LEFT_BRACKET, symbol("print"), OPEN_LIST, dec("12"), CLOSE_LIST, RIGHT_BRACKET).expect(c("12"), s(SYMBOL_LIST, 1), s("print", 1));
		given(OPEN_LIST, LEFT_BRACKET, symbol("print"), dec("12"), RIGHT_BRACKET, CLOSE_LIST).expect(c("12"), s("print", 1), s(SYMBOL_LIST, 1));
	}

	@Test
	public void testCommaWhitespace() {
		given(LEFT_BRACKET, symbol("a"), dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), s("a", 2));
		given(LEFT_BRACKET, symbol("a"), COMMA, dec("12"), dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), s("a", 2));
		given(LEFT_BRACKET, symbol("a"), dec("12"), COMMA, dec("34"), RIGHT_BRACKET).expect(c("12"), c("34"), s("a", 2));

		given(OPEN_LIST, symbol("a"), COMMA, dec("12"), COMMA, dec("34"), CLOSE_LIST).expect(s("a", 0), c("12"), c("34"), s(SYMBOL_LIST, 3));

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
		given(LEFT_BRACKET, OP_MINUS, OPEN_LIST, dec("12"), dec("34"), CLOSE_LIST, RIGHT_BRACKET).expect(c("12"), c("34"), s(SYMBOL_LIST, 2), UNARY_MINUS);
	}

	@Test
	public void testBinaryOperatorWithNestedArgs() {
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, OP_MINUS, dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(c("12"), UNARY_MINUS, c("34"), MINUS);
		given(LEFT_BRACKET, OP_MINUS, LEFT_BRACKET, symbol("a"), dec("34"), RIGHT_BRACKET, OPEN_LIST, dec("56"), CLOSE_LIST, RIGHT_BRACKET).expect(c("34"), s("a", 1), c("56"), s(SYMBOL_LIST, 1), MINUS);
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
	public void testValueQuote() {
		given(QUOTE, dec("12")).expect(OPEN_ROOT_QUOTE, valueMarker("12"), CLOSE_ROOT_QUOTE);
		given(QUOTE, string("abc")).expect(OPEN_ROOT_QUOTE, valueMarker("abc"), CLOSE_ROOT_QUOTE);
	}

	@Test
	public void testNestedEmptyQuote() {
		given(QUOTE, LEFT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
	}

	@Test
	public void testNestedValueQuote() {
		given(QUOTE, LEFT_BRACKET, dec("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
		given(QUOTE, LEFT_BRACKET, dec("12"), oct("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, valueMarker("12"), valueMarker("34"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
	}

	@Test
	public void testRawValueQuote() {
		given(QUOTE, symbol("12")).expect(OPEN_ROOT_QUOTE, rawValueMarker(symbol("12")), CLOSE_ROOT_QUOTE);
		given(QUOTE, OP_PLUS).expect(OPEN_ROOT_QUOTE, rawValueMarker(OP_PLUS), CLOSE_ROOT_QUOTE);
		given(QUOTE, mod(".")).expect(OPEN_ROOT_QUOTE, rawValueMarker(mod(".")), CLOSE_ROOT_QUOTE);
	}

	@Test
	public void testNestedRawValueQuote() {
		given(QUOTE, LEFT_BRACKET, symbol("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, rawValueMarker(symbol("12")), CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
		given(QUOTE, LEFT_BRACKET, symbol("12"), OP_PLUS, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, rawValueMarker(symbol("12")), rawValueMarker(OP_PLUS), CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
		given(QUOTE, LEFT_BRACKET, symbol("12"), mod("."), OP_PLUS, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, rawValueMarker(symbol("12")), rawValueMarker(mod(".")), rawValueMarker(OP_PLUS), CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
	}

	@Test
	public void testDoubleNestedQuote() {
		given(QUOTE, LEFT_BRACKET, LEFT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, OPEN_QUOTE, CLOSE_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
		given(QUOTE, LEFT_BRACKET, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
		given(QUOTE, LEFT_BRACKET, dec("0"), LEFT_BRACKET, dec("12"), RIGHT_BRACKET, dec("3"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, valueMarker("0"), OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, valueMarker("3"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE);
	}

	@Test
	public void testQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), QUOTE, dec("12"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, valueMarker("12"), CLOSE_ROOT_QUOTE, s("test", 1));
		given(LEFT_BRACKET, symbol("test"), QUOTE, symbol("12"), dec("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, rawValueMarker(symbol("12")), CLOSE_ROOT_QUOTE, c("34"), s("test", 2));
	}

	@Test
	public void testNestedQuoteAsArg() {
		given(LEFT_BRACKET, symbol("test"), QUOTE, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE, s("test", 1));
		given(LEFT_BRACKET, symbol("test"), QUOTE, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, dec("34"), RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE, c("34"), s("test", 2));
		given(LEFT_BRACKET, symbol("test"), QUOTE, LEFT_BRACKET, dec("12"), RIGHT_BRACKET, LEFT_BRACKET, symbol("sqrt"), dec("34"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_ROOT_QUOTE, OPEN_QUOTE, valueMarker("12"), CLOSE_QUOTE, CLOSE_ROOT_QUOTE, c("34"), s("sqrt", 1), s("test", 2));
	}
}
