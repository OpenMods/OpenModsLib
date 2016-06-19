package openmods.calc;

import java.math.BigInteger;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.Calculator.ExprType;
import openmods.calc.types.bigint.BigIntCalculator;
import org.junit.Test;

public class BigIntCalculatorTest {

	private final BigIntCalculator sut = new BigIntCalculator();

	public CalcCheck<BigInteger> infix(String value) {
		final IExecutable<BigInteger> expr = sut.compile(ExprType.INFIX, value);
		return new CalcCheck<BigInteger>(sut, expr);
	}

	public CalcCheck<BigInteger> postfix(String value) {
		final IExecutable<BigInteger> expr = sut.compile(ExprType.POSTFIX, value);
		return new CalcCheck<BigInteger>(sut, expr);
	}

	public static BigInteger v(long value) {
		return BigInteger.valueOf(value);
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(v(3)).expectEmptyStack();
		postfix("2 3 *").expectResult(v(6)).expectEmptyStack();
		postfix("10 2 /").expectResult(v(5)).expectEmptyStack();
		postfix("2 5 **").expectResult(v(32)).expectEmptyStack();
		postfix("0b010 0b101 |").expectResult(v(7)).expectEmptyStack();
		postfix("0b100 0b101 &").expectResult(v(4)).expectEmptyStack();
		postfix("0b110 0b101 ^").expectResult(v(3)).expectEmptyStack();
		postfix("0b100 2 <<").expectResult(v(16)).expectEmptyStack();
		postfix("0b100 2 >>").expectResult(v(1)).expectEmptyStack();
		postfix("45 4 %").expectResult(v(1)).expectEmptyStack();
	}

	@Test
	public void testPostfixStackOperations() {
		postfix("2 dup +").expectResult(v(4)).expectEmptyStack();
		postfix("1 2 3 pop +").expectResult(v(3)).expectEmptyStack();
		postfix("2 3 swap -").expectResult(v(1)).expectEmptyStack();
	}

	@Test
	public void testPostfixDupWithArgs() {
		postfix("0 1 2 dup@2").execute().expectStack(v(0), v(1), v(2), v(1), v(2));
	}

	@Test
	public void testPostfixDupWithReturns() {
		postfix("1 2 dup@,4").execute().expectStack(v(1), v(2), v(2), v(2), v(2));
	}

	@Test
	public void testPostfixDupWithArgsAndReturns() {
		postfix("1 2 3 4 dup@3,5").execute().expectStack(v(1), v(2), v(3), v(4), v(2), v(3));
	}

	@Test
	public void testPostfixPopWithArgs() {
		postfix("1 2 3 4 pop@3").execute().expectStack(v(1));
	}

	@Test
	public void testVariadicPostfixFunctions() {
		postfix("max@0").expectResult(v(0)).expectEmptyStack();
		postfix("1 max@1").expectResult(v(1)).expectEmptyStack();
		postfix("1 2 max@2").expectResult(v(2)).expectEmptyStack();

		postfix("3 2 1 sum@3").expectResult(v(6)).expectEmptyStack();
		postfix("3 2 1 avg@3").expectResult(v(2)).expectEmptyStack();
	}

	@Test
	public void testBasicInfix() {
		infix("1+2").expectResult(v(3)).expectEmptyStack();
		infix("2*3").expectResult(v(6)).expectEmptyStack();
		infix("10/2").expectResult(v(5)).expectEmptyStack();
		infix("2**5").expectResult(v(32)).expectEmptyStack();
		infix("2(5)").expectResult(v(10)).expectEmptyStack();
		infix("0b010|0b101").expectResult(v(7)).expectEmptyStack();
		infix("0b100&0b101").expectResult(v(4)).expectEmptyStack();
		infix("0b110^0b101").expectResult(v(3)).expectEmptyStack();
		infix("0b100<<2").expectResult(v(16)).expectEmptyStack();
		infix("0b100>>2").expectResult(v(1)).expectEmptyStack();
		infix("45 % 4").expectResult(v(1)).expectEmptyStack();
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(v(0)).expectEmptyStack();

		infix("1 + 2 * 3").expectResult(v(7)).expectEmptyStack();
		infix("1 + (2 * 3)").expectResult(v(7)).expectEmptyStack();
		infix("(1 + 2) * 3").expectResult(v(9)).expectEmptyStack();
		infix("-(1 + 2) * 3").expectResult(v(-9)).expectEmptyStack();
		infix("(1 + 2) * -3").expectResult(v(-9)).expectEmptyStack();

		infix("--3").expectResult(v(3)).expectEmptyStack();
		infix("-~2").expectResult(v(3)).expectEmptyStack();

		infix("2 * 2 ** 2").expectResult(v(8)).expectEmptyStack();
		infix("2 * (2 ** 2)").expectResult(v(8)).expectEmptyStack();
		infix("(2 * 2) ** 2").expectResult(v(16)).expectEmptyStack();
	}

	@Test
	public void testBasicInfixFunctions() {
		infix("gcd(6, 8)").expectResult(v(2)).expectEmptyStack();
		infix("abs(-2)").expectResult(v(2)).expectEmptyStack();
		infix("5*abs(-2)").expectResult(v(10)).expectEmptyStack();
		infix("1+abs(-2)").expectResult(v(3)).expectEmptyStack();
		infix("min(2,3)").expectResult(v(2)).expectEmptyStack();
		infix("max(2,3)").expectResult(v(3)).expectEmptyStack();
		infix("2-max(2,3)").expectResult(v(-1)).expectEmptyStack();
	}

	@Test
	public void testVariadicInfixFunctions() {
		infix("max()").expectResult(v(0)).expectEmptyStack();
		infix("max(1)").expectResult(v(1)).expectEmptyStack();
		infix("max(1,2)").expectResult(v(2)).expectEmptyStack();
		infix("max(3,2,1)").expectResult(v(3)).expectEmptyStack();
	}

	@Test(expected = Exception.class)
	public void testTooManyParameters() {
		infix("abs(0, 1)").execute();
	}

	@Test(expected = Exception.class)
	public void testTooFewParameters() {
		infix("gcd(0)").execute();
	}
}
