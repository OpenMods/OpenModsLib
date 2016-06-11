package openmods.calc;

import java.math.BigInteger;
import openmods.calc.types.bigint.BigIntPrinter;
import org.junit.Assert;
import org.junit.Test;

public class BigIntPrinterTest {

	public static void test(String expected, int radix, BigInteger input) {
		final BigIntPrinter printer = new BigIntPrinter();
		Assert.assertEquals(expected, printer.toString(input, radix));
	}

	public static void test(String expected, int radix, long input) {
		test(expected, radix, BigInteger.valueOf(input));
	}

	@Test
	public void testZero() {
		test("0", 2, BigInteger.ZERO);
		test("0", 10, BigInteger.ZERO);
		test("0", 16, BigInteger.ZERO);
	}

	@Test
	public void testOne() {
		test("1", 2, BigInteger.ONE);
		test("1", 10, BigInteger.ONE);
		test("1", 16, BigInteger.ONE);
	}

	@Test
	public void testOneZero() {
		test("10", 2, 2);
		test("10", 10, 10);
		test("10", 16, 16);
	}

	@Test
	public void testAlphanumeric() {
		test("a", 16, 10);
		test("z", 36, 35);
	}

	@Test
	public void testQuoted() {
		test("'36'", 37, 36);
	}

	@Test
	public void testDoubleQuoted() {
		test("'36\"37'", 38, 36 * 38 + 37);
	}

	@Test
	public void testQuotedNoQuoted() {
		test("z'36\"37'", 38, ((35 * 38) + 36) * 38 + 37);
		test("'36'z'37'", 38, ((36 * 38) + 35) * 38 + 37);
		test("'36\"37'z", 38, ((36 * 38) + 37) * 38 + 35);
	}

	@Test
	public void testNegativeOne() {
		test("-1", 2, -1);
		test("-1", 10, -1);
		test("-1", 16, -1);
	}

	@Test
	public void testNegativeOneZero() {
		test("-10", 2, -2);
		test("-10", 10, -10);
		test("-10", 16, -16);
	}

	@Test
	public void testNegativeDoubleQuoted() {
		test("-'36\"37'", 38, -(36 * 38 + 37));
	}
}
