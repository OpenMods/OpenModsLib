package openmods.calc;

import openmods.calc.CalcTestUtils.ValueParserHelper;
import openmods.calc.types.fraction.FractionParser;
import org.apache.commons.lang3.math.Fraction;
import org.junit.Test;

public class FractionParserTest {

	public final FractionParser parser = new FractionParser();

	private final ValueParserHelper<Fraction> helper = new ValueParserHelper<Fraction>(parser);

	private static Fraction f(int value) {
		return Fraction.getFraction(value, 1);
	}

	private static Fraction f(int n, int d) {
		return Fraction.getReducedFraction(n, d);
	}

	@Test
	public void testBinary() {
		helper.testBin(Fraction.ZERO, "0");
		helper.testBin(Fraction.ONE, "1");
		helper.testBin(Fraction.ZERO, "00");
		helper.testBin(Fraction.ONE, "01");
		helper.testBin(f(2), "10");
		helper.testBin(f(3), "11");
		helper.testBin(f(4), "100");
		helper.testBin(f(7), "111");

		helper.testBin(f(1, 2), "0.1");
		helper.testBin(f(3, 4), "0.11");
		helper.testBin(f(1, 4), "0.01");
		helper.testBin(f(7, 8), "0.111");
		helper.testBin(f(1, 8), "0.001");
	}

	@Test
	public void testOctal() {
		helper.testOct(Fraction.ZERO, "0");
		helper.testOct(Fraction.ZERO, "00");
		helper.testOct(Fraction.ONE, "1");
		helper.testOct(Fraction.ONE, "01");
		helper.testOct(f(2), "2");
		helper.testOct(f(7), "7");
		helper.testOct(f(8), "10");
		helper.testOct(f(15), "17");
		helper.testOct(f(16), "20");
		helper.testOct(f(34), "42");
		helper.testOct(f(83), "123");

		helper.testOct(f(1, 8), "0.1");
		helper.testOct(f(7, 8), "0.7");
		helper.testOct(f(1, 64), "0.01");
		helper.testOct(f(63, 64), "0.77");
	}

	@Test
	public void testDecimal() {
		helper.testDec(Fraction.ZERO, "0");
		helper.testDec(Fraction.ZERO, "00");
		helper.testDec(Fraction.ONE, "1");
		helper.testDec(Fraction.ONE, "01");
		helper.testDec(f(2), "2");
		helper.testDec(f(9), "9");
		helper.testDec(f(10), "10");
		helper.testDec(f(19), "19");
		helper.testDec(f(20), "20");
		helper.testDec(f(42), "42");
		helper.testDec(f(123), "123");

		helper.testDec(f(1, 10), "0.1");
		helper.testDec(f(9, 10), "0.9");
		helper.testDec(f(1, 100), "0.01");
		helper.testDec(f(99, 100), "0.99");
	}

	@Test
	public void testHexadecimal() {
		helper.testHex(Fraction.ZERO, "0");
		helper.testHex(Fraction.ZERO, "00");
		helper.testHex(Fraction.ONE, "1");
		helper.testHex(Fraction.ONE, "01");
		helper.testHex(f(2), "2");
		helper.testHex(f(10), "A");
		helper.testHex(f(15), "F");
		helper.testHex(f(16), "10");
		helper.testHex(f(31), "1F");
		helper.testHex(f(32), "20");
		helper.testHex(f(66), "42");
		helper.testHex(f(419), "1A3");

		helper.testHex(f(1, 16), "0.1");
		helper.testHex(f(15, 16), "0.F");
		helper.testHex(f(1, 256), "0.01");
		helper.testHex(f(255, 256), "0.FF");
	}

	@Test
	public void testQuotedDecimalZeros() {
		helper.testQuoted(Fraction.ZERO, "2#0");
		helper.testQuoted(Fraction.ZERO, "2#00");

		helper.testQuoted(Fraction.ZERO, "3#0");
		helper.testQuoted(Fraction.ZERO, "8#0");
	}

	@Test
	public void testQuotedIntegerOnes() {
		helper.testQuoted(Fraction.ONE, "2#1");
		helper.testQuoted(Fraction.ONE, "2#01");

		helper.testQuoted(Fraction.ONE, "3#1");
		helper.testQuoted(Fraction.ONE, "8#1");
	}

	@Test
	public void testQuotedIntegerTens() {
		helper.testQuoted(f(2), "2#10");
		helper.testQuoted(f(3), "3#10");
		helper.testQuoted(f(4), "4#10");
		helper.testQuoted(f(5), "5#10");
		helper.testQuoted(f(10), "10#10");
		helper.testQuoted(f(16), "16#10");
	}

	@Test
	public void testQuotedFractionBases() {
		helper.testQuoted(f(1, 2), "2#0.1");
		helper.testQuoted(f(1, 3), "3#0.1");
		helper.testQuoted(f(1, 4), "4#0.1");
		helper.testQuoted(f(1, 5), "5#0.1");
		helper.testQuoted(f(1, 10), "10#0.1");
		helper.testQuoted(f(1, 16), "16#0.1");
	}

	@Test
	public void testQuotedIntegerA() {
		helper.testQuoted(f(10), "11#A");
		helper.testQuoted(f(10), "12#A");
		helper.testQuoted(f(10), "13#A");
		helper.testQuoted(f(10), "14#A");
		helper.testQuoted(f(10), "15#A");
		helper.testQuoted(f(10), "16#A");
	}

	@Test
	public void testQuotedIntegerSingleLargeDigit() {
		helper.testQuoted(f(10), "11#'10'");
		helper.testQuoted(f(10), "12#'10'");
		helper.testQuoted(f(10), "13#'10'");
		helper.testQuoted(f(10), "14#'10'");
		helper.testQuoted(f(10), "15#'10'");
		helper.testQuoted(f(10), "16#'10'");
	}

	@Test
	public void testQuotedFractionSingleLargeDigit() {
		helper.testQuoted(f(10, 11), "11#0.'10'");
		helper.testQuoted(f(10, 12), "12#0.'10'");
		helper.testQuoted(f(10, 13), "13#0.'10'");
		helper.testQuoted(f(10, 14), "14#0.'10'");
		helper.testQuoted(f(10, 15), "15#0.'10'");
		helper.testQuoted(f(10, 16), "16#0.'10'");
	}

	@Test
	public void testQuotedIntegerMultipleLargeDigitsSingleQuotes() {
		helper.testQuoted(f(9 * 11 + 10), "11#'9''10'");
		helper.testQuoted(f((9 * 12 + 10) * 12 + 11), "12#'9''10''11'");
		helper.testQuoted(f(((9 * 13 + 10) * 13 + 11) * 13 + 12), "13#'9''10''11''12'");

		helper.testQuoted(f(9 * 13 + 10 * 1).add(f(11, 13)).add(f(12, (13 * 13))), "13#'9''10'.'11''12'");
	}

	@Test
	public void testQuotedIntegerMultipleLargeDigitsDoubleQuotes() {
		helper.testQuoted(f(9 * 11 + 10), "11#'9\"10'");
		helper.testQuoted(f((9 * 12 + 10) * 12 + 11), "12#'9\"10\"11'");
		helper.testQuoted(f(((9 * 13 + 10) * 13 + 11) * 13 + 12), "13#'9\"10\"11\"12'");
	}

	@Test
	public void testQuotedIntegerMixedDigits() {
		helper.testQuoted(f(666), "16#29'10'");
		helper.testQuoted(f((11 * 16 + 5) * 16 + 10), "16#'11'5'10'");
		helper.testQuoted(f(16 * 16 * 16), "16#'1'0'0'0");
		helper.testQuoted(f((10 * 16 + 11) * 16 + 12), "16#'10\"11'C");
	}
}
