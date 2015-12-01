package openmods.calc;

import openmods.calc.Calculator.ExprType;

import org.junit.Assert;
import org.junit.Test;

public class DoubleCalculatorTest {

	private final DoubleCalculator sut = new DoubleCalculator();

	public class Check {
		private final TopFrame<Double> frame = new TopFrame<Double>();

		private final IExecutable<Double> expr;

		public Check(IExecutable<Double> expr) {
			this.expr = expr;
		}

		public Check push(Double... args) {
			for (Double arg : args)
				frame.stack().push(arg);

			return this;
		}

		public Check expectResult(double value) {
			Assert.assertEquals(value, sut.executeAndPop(expr).doubleValue(), 0.0001);
			return this;
		}

		public Check expectEmptyStack() {
			Assert.assertTrue(frame.stack().isEmpty());
			return this;
		}

		public Check execute() {
			sut.execute(expr);
			return this;
		}
	}

	public Check infix(String value) {
		final IExecutable<Double> expr = sut.compile(ExprType.INFIX, value);
		return new Check(expr);
	}

	public Check postfix(String value) {
		final IExecutable<Double> expr = sut.compile(ExprType.POSTFIX, value);
		return new Check(expr);
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(3).expectEmptyStack();
		postfix("2 3 *").expectResult(6).expectEmptyStack();
		postfix("10 2 /").expectResult(5).expectEmptyStack();
		postfix("2 5 ^").expectResult(32).expectEmptyStack();
	}

	@Test
	public void testPostfixStackOperations() {
		postfix("2 dup +").expectResult(4).expectEmptyStack();
		postfix("1 2 3 pop +").expectResult(3).expectEmptyStack();
		postfix("2 3 swap -").expectResult(1).expectEmptyStack();
	}

	@Test
	public void testBasicInfix() {
		infix("1 + 2").expectResult(3).expectEmptyStack();
		infix("2 * 3").expectResult(6).expectEmptyStack();
		infix("10 / 2").expectResult(5).expectEmptyStack();
		infix("2 ^ 5").expectResult(32).expectEmptyStack();
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(0).expectEmptyStack();

		infix("1 + 2 * 3").expectResult(7).expectEmptyStack();
		infix("1 + (2 * 3)").expectResult(7).expectEmptyStack();
		infix("(1 + 2) * 3").expectResult(9).expectEmptyStack();

		infix("2 * 2 ^ 2").expectResult(8).expectEmptyStack();
		infix("2 * (2 ^ 2)").expectResult(8).expectEmptyStack();
		infix("(2 * 2) ^ 2").expectResult(16).expectEmptyStack();
	}

	@Test
	public void testBasicInfixFunctions() {
		infix("sin(0)").expectResult(0).expectEmptyStack();
		infix("cos(0)").expectResult(1).expectEmptyStack();
		infix("1+cos(0)").expectResult(2).expectEmptyStack();
		infix("min(2,3)").expectResult(2).expectEmptyStack();
		infix("max(2,3)").expectResult(3).expectEmptyStack();
		infix("2-max(2,3)").expectResult(-1).expectEmptyStack();
	}

	@Test(expected = StackValidationException.class)
	public void testTooManyParameters() {
		infix("sin(0, 1)").execute();
	}

	@Test(expected = StackValidationException.class)
	public void testTooFewParameters() {
		infix("min(0)").execute();
	}
}
