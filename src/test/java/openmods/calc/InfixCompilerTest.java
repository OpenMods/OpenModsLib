package openmods.calc;

import static openmods.calc.CalcTestUtils.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class InfixCompilerTest {

	public final OperatorDictionary<String> operators = new OperatorDictionary<String>();
	{
		operators.registerMixedOperator("+", PLUS, UNARY_PLUS);
		operators.registerMixedOperator("-", MINUS, UNARY_MINUS);
		operators.registerUnaryOperator("!", UNARY_NEG);
		operators.registerBinaryOperator("*", MULTIPLY);
	}

	private class ResultTester {
		private final InfixCompiler<?> compiler = new InfixCompiler<String>(VALUE_PARSER, operators);

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
	public void testSimpleExpr() {
		given(dec("1"), OP_PLUS, dec("2"))
				.expect(c("1"), c("2"), PLUS);
	}

	@Test
	public void testMultipleSamePrecendenceOps() {
		given(dec("1"), OP_PLUS, dec("2"), OP_MINUS, dec("3"))
				.expect(c("1"), c("2"), PLUS, c("3"), MINUS);
	}

	@Test
	public void testMultipleDifferentPrecendenceOps() {
		given(dec("1"), OP_PLUS, dec("2"), OP_MULTIPLY, dec("3"))
				.expect(c("1"), c("2"), c("3"), MULTIPLY, PLUS);
		given(dec("1"), OP_MULTIPLY, dec("2"), OP_PLUS, dec("3"))
				.expect(c("1"), c("2"), MULTIPLY, c("3"), PLUS);
	}

	@Test
	public void testBrackets() {
		given(LEFT_BRACKET, dec("1"), OP_PLUS, dec("2"), RIGHT_BRACKET, OP_MULTIPLY, dec("3"))
				.expect(c("1"), c("2"), PLUS, c("3"), MULTIPLY);
		given(dec("1"), OP_MULTIPLY, LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), c("3"), PLUS, MULTIPLY);
	}

	@Test
	public void testNestedBrackets() {
		given(LEFT_BRACKET, dec("1"), OP_PLUS, LEFT_BRACKET, dec("2"), OP_MINUS, LEFT_BRACKET, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("1"), c("2"), c("3"), MINUS, PLUS);

		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("1"), RIGHT_BRACKET, OP_PLUS, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), PLUS, c("3"), MINUS);
	}

	@Test
	public void testNullaryFunction() {
		given(symbol("pi"))
				.expect(s("pi", 0));

		given(symbol("pi"), OP_PLUS, dec("2"))
				.expect(s("pi", 0), c("2"), PLUS);

		given(dec("2"), OP_PLUS, symbol("pi"))
				.expect(c("2"), s("pi", 0), PLUS);

		given(symbol("a"), OP_PLUS, symbol("b"))
				.expect(s("a", 0), s("b", 0), PLUS);

		given(symbol("pi"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(s("pi", 0));

		given(symbol("pi"), LEFT_BRACKET, RIGHT_BRACKET, OP_PLUS, symbol("e"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(s("pi", 0), s("e", 0), PLUS);

		given(symbol("sin"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(s("a", 0), s("sin", 1));
	}

	@Test
	public void testUnaryFunction() {
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), s("sin", 1));

		given(symbol("sin"), LEFT_BRACKET, LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("2"), s("sin", 1));

		given(symbol("sin"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), PLUS, s("sin", 1));

		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), s("sin", 1), c("3"), s("cos", 1), PLUS);
	}

	@Test
	public void testBinaryFunction() {
		given(symbol("exp"), LEFT_BRACKET, dec("2"), COMMA, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), s("exp", 2));

		given(symbol("exp"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), COMMA, dec("4"), OP_MINUS, dec("5"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), PLUS, c("4"), c("5"), MINUS, s("exp", 2));
	}

	@Test
	public void testFunctionSum() {
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("exp"), LEFT_BRACKET, dec("3"), COMMA, dec("4"), RIGHT_BRACKET)
				.expect(c("2"), s("sin", 1), c("3"), c("4"), s("exp", 2), PLUS);
	}

	@Test
	public void testNestedFunctions() {
		given(symbol("sin"), LEFT_BRACKET, symbol("cos"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("3"), s("cos", 1), s("sin", 1));

		given(symbol("exp"), LEFT_BRACKET, symbol("sin"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, COMMA, dec("4"), OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("3"), s("sin", 1), c("4"), c("2"), s("cos", 1), PLUS, s("exp", 2));
	}

	@Test
	public void testImmediateSymbols() {
		given(symbol("a"))
				.expect(s("a", 0));

		given(symbol("a"), OP_PLUS, symbol("b"))
				.expect(s("a", 0), s("b", 0), PLUS);

		given(symbol("sin"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(s("a", 0), s("sin", 1));

		given(symbol("min"), LEFT_BRACKET, symbol("a"), COMMA, symbol("$b"), RIGHT_BRACKET)
				.expect(s("a", 0), s("$b", 0), s("min", 2));
	}

	@Test
	public void testUnaryOperators() {
		given(OP_MINUS, dec("2"))
				.expect(c("2"), UNARY_MINUS);

		given(OP_NEG, dec("3"))
				.expect(c("3"), UNARY_NEG);

		given(OP_MINUS, dec("2"))
				.expect(c("2"), UNARY_MINUS);

		given(dec("1"), OP_MINUS, OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), UNARY_MINUS, MINUS);
		given(dec("1"), OP_MULTIPLY, OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), UNARY_MINUS, MULTIPLY);
		given(dec("1"), OP_MULTIPLY, LEFT_BRACKET, OP_MINUS, dec("2"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), UNARY_MINUS, MULTIPLY);

		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"))
				.expect(c("2"), c("3"), MINUS);

		given(OP_MINUS, symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), s("sin", 1), UNARY_MINUS);
	}

	@Test
	public void testDoubleUnaryOperators() {
		given(OP_MINUS, OP_MINUS, dec("2"))
				.expect(c("2"), UNARY_MINUS, UNARY_MINUS);

		given(OP_MINUS, OP_PLUS, dec("2"))
				.expect(c("2"), UNARY_PLUS, UNARY_MINUS);

		given(OP_MINUS, OP_NEG, dec("2"))
				.expect(c("2"), UNARY_NEG, UNARY_MINUS);

		given(OP_NEG, OP_MINUS, OP_PLUS, dec("2"))
				.expect(c("2"), UNARY_PLUS, UNARY_MINUS, UNARY_NEG);
	}
}
