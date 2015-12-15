package openmods.calc;

import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.Calculator.ExprType;

import org.apache.commons.lang3.math.Fraction;
import org.junit.Test;

public class FractionCalculatorTest {

	private final FractionCalculator sut = new FractionCalculator();

	public CalcCheck<Fraction> infix(String value) {
		final IExecutable<Fraction> expr = sut.compile(ExprType.INFIX, value);
		return new CalcCheck<Fraction>(sut, expr);
	}

	public CalcCheck<Fraction> postfix(String value) {
		final IExecutable<Fraction> expr = sut.compile(ExprType.POSTFIX, value);
		return new CalcCheck<Fraction>(sut, expr);
	}

	public static Fraction f(int value) {
		return Fraction.getFraction(value, 1);
	}

	public static Fraction f(int numerator, int denominator) {
		return Fraction.getFraction(numerator, denominator);
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(f(3)).expectEmptyStack();
		postfix("2 3 *").expectResult(f(6)).expectEmptyStack();
		postfix("10 2 /").expectResult(f(5)).expectEmptyStack();
		postfix("1 2 / 1 2 / +").expectResult(f(1)).expectEmptyStack();
		postfix("1 2 / 1 +").expectResult(f(3, 2)).expectEmptyStack();
	}

	@Test
	public void testPostfixStackOperations() {
		postfix("2 dup +").expectResult(f(4)).expectEmptyStack();
		postfix("1 2 3 pop +").expectResult(f(3)).expectEmptyStack();
		postfix("2 3 swap -").expectResult(f(1)).expectEmptyStack();
	}

	@Test
	public void testPostfixDupWithArgs() {
		postfix("0 1 2 dup@2").execute().expectStack(f(0), f(1), f(2), f(1), f(2));
	}

	@Test
	public void testPostfixDupWithReturns() {
		postfix("1 2 dup@,4").execute().expectStack(f(1), f(2), f(2), f(2), f(2));
	}

	@Test
	public void testPostfixDupWithArgsAndReturns() {
		postfix("1 2 3 4 dup@3,5").execute().expectStack(f(1), f(2), f(3), f(4), f(2), f(3));
	}

	@Test
	public void testPostfixPopWithArgs() {
		postfix("1 2 3 4 pop@3").execute().expectStack(f(1));
	}

	@Test
	public void testVariadicPostfixFunctions() {
		postfix("max@0").expectResult(f(0)).expectEmptyStack();
		postfix("1 max@1").expectResult(f(1)).expectEmptyStack();
		postfix("1 2 max@2").expectResult(f(2)).expectEmptyStack();
		postfix("3 2 1 max@3").expectResult(f(3)).expectEmptyStack();
	}

	@Test
	public void testBasicInfix() {
		infix("1 + 2").expectResult(f(3)).expectEmptyStack();
		infix("2 * 3").expectResult(f(6)).expectEmptyStack();
		infix("10 / 2").expectResult(f(5)).expectEmptyStack();
		infix("1/2 + 1/2").expectResult(f(1)).expectEmptyStack();
		infix("0.5 + 1/2").expectResult(f(1)).expectEmptyStack();
		infix("-3/2").expectResult(f(-3, 2)).expectEmptyStack();
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(f(0)).expectEmptyStack();

		infix("1 + 2 * 3").expectResult(f(7)).expectEmptyStack();
		infix("1 + (2 * 3)").expectResult(f(7)).expectEmptyStack();
		infix("(1 + 2) * 3").expectResult(f(9)).expectEmptyStack();
		infix("-(1 + 2) * 3").expectResult(f(-9)).expectEmptyStack();
		infix("(1 + 2) * -3").expectResult(f(-9)).expectEmptyStack();
		infix("--3").expectResult(f(3)).expectEmptyStack();
	}

	@Test
	public void testBasicInfixFunctions() {
		infix("min(2,3)").expectResult(f(2)).expectEmptyStack();
		infix("max(2,3)").expectResult(f(3)).expectEmptyStack();
		infix("sqrt(9/4)").expectResult(f(3, 2)).expectEmptyStack();
		infix("2-max(2,3)").expectResult(f(-1)).expectEmptyStack();
		infix("int(7/3)").expectResult(f(2)).expectEmptyStack();
		infix("frac(7/3)").expectResult(f(1, 3)).expectEmptyStack();
		infix("numerator(7/3)").expectResult(f(7)).expectEmptyStack();
		infix("denominator(7/3)").expectResult(f(3)).expectEmptyStack();
	}

	@Test
	public void testVariadicInfixFunctions() {
		infix("max()").expectResult(f(0)).expectEmptyStack();
		infix("max(1)").expectResult(f(1)).expectEmptyStack();
		infix("max(1,2)").expectResult(f(2)).expectEmptyStack();
		infix("max(3,2,1)").expectResult(f(3)).expectEmptyStack();
	}

	@Test(expected = Exception.class)
	public void testTooManyParameters() {
		infix("abs(0, 1)").execute();
	}
}
