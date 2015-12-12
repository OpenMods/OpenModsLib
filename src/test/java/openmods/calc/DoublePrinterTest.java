package openmods.calc;

import org.junit.Assert;
import org.junit.Test;

public class DoublePrinterTest {
	public static void test(String expected, int radix, double input) {
		final DoublePrinter printer = new DoublePrinter(5);
		Assert.assertEquals(expected, printer.toString(input, radix));
	}

	@Test
	public void testZero() {
		test("0", 2, 0);
		test("0", 10, 0);
		test("0", 16, 0);
	}

	@Test
	public void testOne() {
		test("1", 2, 1);
		test("1", 10, 1);
		test("1", 16, 1);
	}

	@Test
	public void testZeroDotOne() {
		test("0.1", 2, 0.5);
		test("0.1", 10, 0.1);
		test("0.1", 16, 1.0 / 16.0);
	}

	@Test
	public void testZeroDotZeroOne() {
		test("0.01", 2, 0.25);
		test("0.01", 10, 0.01);
		test("0.01", 16, 1.0 / 256.0);
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
	public void testFractionalAlphanumeric() {
		test("0.a", 16, 10.0 / 16.0);
		test("0.z", 36, 35.0 / 36.0);
	}

	@Test
	public void testQuoted() {
		test("'36'", 37, 36);
	}

	@Test
	public void testFractionalQuoted() {
		test("0.'36'", 37, 36.0 / 37.0);
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
	public void testNegativeZeroDotOne() {
		test("-0.1", 2, -0.5);
		test("-0.1", 10, -0.1);
		test("-0.1", 16, -1.0 / 16.0);
	}

	@Test
	public void testNegativeDoubleQuoted() {
		test("-'36\"37'", 38, -(36 * 38 + 37));
	}
}
