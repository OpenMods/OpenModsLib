package openmods.calc;

import static openmods.calc.TokenUtils.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class InfixCompilerTest {

	private static class DummyOperator<E> extends Operator<E> {
		private final String id;

		public DummyOperator(int precendence, String id) {
			super(precendence);
			this.id = id;
		}

		@Override
		public void execute(CalculatorContext<E> context) {}

		@Override
		public String toString() {
			return "Operator [" + id + "]";
		}

	}

	private static final IOperator<String> PLUS = new DummyOperator<String>(1, "+");

	private static final Token OP_PLUS = op("+");

	private static final IOperator<String> UNARY_PLUS = new DummyOperator<String>(4, "u+");

	private static final IOperator<String> MINUS = new DummyOperator<String>(1, "-");

	private static final Token OP_MINUS = op("-");

	private static final IOperator<String> UNARY_MINUS = new DummyOperator<String>(4, "u-");

	private static final IOperator<String> MULTIPLY = new DummyOperator<String>(2, "*");

	private static final Token OP_MULTIPLY = op("*");

	private final OperatorDictionary<String> operators = new OperatorDictionary<String>();

	public IExecutable<String> c(String value) {
		return new Constant<String>(value);
	}

	public IExecutable<String> s(String value) {
		return new DelayedSymbol<String>(value);
	}

	{
		operators.registerOperator("+", PLUS, UNARY_PLUS);
		operators.registerOperator("-", MINUS, "neg", UNARY_MINUS);
		operators.registerOperator("*", MULTIPLY);
	}

	private final IValueParser<String> valueParser = new IValueParser<String>() {
		@Override
		public String parseToken(Token token) {
			return token.value;
		}
	};

	private class ResultTester {
		private final InfixCompiler<?> compiler = new InfixCompiler<String>(valueParser, operators);

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
		given(symbol("pi"), LEFT_BRACKET, RIGHT_BRACKET)
				.expect(s("pi"));
	}

	@Test
	public void testUnaryFunction() {
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), s("sin"));

		given(symbol("sin"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), PLUS, s("sin"));
	}

	@Test
	public void testBinaryFunction() {
		given(symbol("exp"), LEFT_BRACKET, dec("2"), COMMA, dec("3"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), s("exp"));

		given(symbol("exp"), LEFT_BRACKET, dec("2"), OP_PLUS, dec("3"), COMMA, dec("4"), OP_MINUS, dec("5"), RIGHT_BRACKET)
				.expect(c("2"), c("3"), PLUS, c("4"), c("5"), MINUS, s("exp"));
	}

	@Test
	public void testFunctionSum() {
		given(symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_PLUS, symbol("exp"), LEFT_BRACKET, dec("3"), COMMA, dec("4"), RIGHT_BRACKET)
				.expect(c("2"), s("sin"), c("3"), c("4"), s("exp"), PLUS);
	}

	@Test
	public void testNestedFunctions() {
		given(symbol("exp"), LEFT_BRACKET, symbol("sin"), LEFT_BRACKET, dec("3"), RIGHT_BRACKET, COMMA, dec("4"), OP_PLUS, symbol("cos"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, RIGHT_BRACKET)
				.expect(c("3"), s("sin"), c("4"), c("2"), s("cos"), PLUS, s("exp"));
	}

	@Test
	public void testImmediateSymbols() {
		given(symbol("a"))
				.expect(s("a"));

		given(symbol("a"), OP_PLUS, symbol("b"))
				.expect(s("a"), s("b"), PLUS);

		given(symbol("sin"), LEFT_BRACKET, symbol("a"), RIGHT_BRACKET)
				.expect(s("a"), s("sin"));

		given(symbol("min"), LEFT_BRACKET, symbol("a"), COMMA, symbol("$b"), RIGHT_BRACKET)
				.expect(s("a"), s("$b"), s("min"));
	}

	@Test
	public void testConstSymbols() {
		given(constant("a"))
				.expect(s("a"));

		given(constant("a"), OP_PLUS, constant("b"))
				.expect(s("a"), s("b"), PLUS);

		given(symbol("sin"), LEFT_BRACKET, constant("a"), RIGHT_BRACKET)
				.expect(s("a"), s("sin"));
	}

	@Test
	public void testUnary() {
		given(dec("1"), OP_MINUS, OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), UNARY_MINUS, MINUS);
		given(dec("1"), OP_MULTIPLY, OP_MINUS, dec("2"))
				.expect(c("1"), c("2"), UNARY_MINUS, MULTIPLY);
		given(dec("1"), OP_MULTIPLY, LEFT_BRACKET, OP_MINUS, dec("2"), RIGHT_BRACKET)
				.expect(c("1"), c("2"), UNARY_MINUS, MULTIPLY);

		given(LEFT_BRACKET, dec("2"), RIGHT_BRACKET, OP_MINUS, dec("3"))
				.expect(c("2"), c("3"), MINUS);

		given(OP_MINUS, symbol("sin"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET)
				.expect(c("2"), s("sin"), UNARY_MINUS);
	}

}
