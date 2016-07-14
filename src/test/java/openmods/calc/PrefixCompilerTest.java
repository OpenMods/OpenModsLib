package openmods.calc;

import java.util.Arrays;
import java.util.List;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.PrefixCompiler;
import openmods.calc.parsing.Token;
import org.junit.Assert;
import org.junit.Test;

public class PrefixCompilerTest extends CalcTestUtils {

	private static final Token CLOSE_LIST = rightBracket("]");
	private static final Token OPEN_LIST = leftBracket("[");

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
		public int numberOfChildren() {
			return children.size();
		}
	}

	public final IExprNodeFactory<String> nodeFactory = new DefaultExprNodeFactory<String>() {
		@Override
		public IExprNode<String> createBracketNode(String openingBracket, List<IExprNode<String>> children) {
			return new TestBracketNode(children);
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
}
