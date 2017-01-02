package openmods.calc;

import java.math.BigInteger;
import openmods.calc.CalcTestUtils.ValueParserHelper;
import openmods.calc.types.bigint.BigIntParser;
import org.junit.Test;

public class BigIntParserTest {

	public final BigIntParser parser = new BigIntParser();

	private final ValueParserHelper<BigInteger> helper = new ValueParserHelper<BigInteger>(parser);

	@Test
	public void testBinary() {
		helper.testBin(BigInteger.ZERO, "0");
		helper.testBin(BigInteger.ONE, "1");
		helper.testBin(BigInteger.ZERO, "00");
		helper.testBin(BigInteger.ZERO, "0_0");
		helper.testBin(BigInteger.ZERO, "0__0");
		helper.testBin(BigInteger.ONE, "01");
		helper.testBin(BigInteger.ONE, "0_1");
		helper.testBin(BigInteger.ONE, "0__1");
		helper.testBin(BigInteger.valueOf(2), "10");
		helper.testBin(BigInteger.valueOf(3), "11");
		helper.testBin(BigInteger.valueOf(4), "100");
		helper.testBin(BigInteger.valueOf(7), "111");
	}

	@Test
	public void testOctal() {
		helper.testOct(BigInteger.ZERO, "0");
		helper.testOct(BigInteger.ZERO, "00");
		helper.testOct(BigInteger.ZERO, "0_0");
		helper.testOct(BigInteger.ZERO, "0__0");
		helper.testOct(BigInteger.ONE, "1");
		helper.testOct(BigInteger.ONE, "01");
		helper.testOct(BigInteger.ONE, "0_1");
		helper.testOct(BigInteger.ONE, "0__1");
		helper.testOct(BigInteger.valueOf(2), "2");
		helper.testOct(BigInteger.valueOf(7), "7");
		helper.testOct(BigInteger.valueOf(8), "10");
		helper.testOct(BigInteger.valueOf(15), "17");
		helper.testOct(BigInteger.valueOf(16), "20");
		helper.testOct(BigInteger.valueOf(34), "42");
		helper.testOct(BigInteger.valueOf(83), "123");
	}

	@Test
	public void testDecimal() {
		helper.testDec(BigInteger.ZERO, "0");
		helper.testDec(BigInteger.ZERO, "00");
		helper.testDec(BigInteger.ZERO, "0_0");
		helper.testDec(BigInteger.ZERO, "0__0");
		helper.testDec(BigInteger.ONE, "1");
		helper.testDec(BigInteger.ONE, "01");
		helper.testDec(BigInteger.ONE, "0_1");
		helper.testDec(BigInteger.ONE, "0__1");
		helper.testDec(BigInteger.valueOf(2), "2");
		helper.testDec(BigInteger.valueOf(9), "9");
		helper.testDec(BigInteger.valueOf(10), "10");
		helper.testDec(BigInteger.valueOf(19), "19");
		helper.testDec(BigInteger.valueOf(20), "20");
		helper.testDec(BigInteger.valueOf(42), "42");
		helper.testDec(BigInteger.valueOf(123), "123");
	}

	@Test
	public void testHexadecimal() {
		helper.testHex(BigInteger.ZERO, "0");
		helper.testHex(BigInteger.ZERO, "00");
		helper.testHex(BigInteger.ZERO, "0_0");
		helper.testHex(BigInteger.ZERO, "0__0");
		helper.testHex(BigInteger.ONE, "1");
		helper.testHex(BigInteger.ONE, "01");
		helper.testHex(BigInteger.ONE, "0_1");
		helper.testHex(BigInteger.ONE, "0__1");
		helper.testHex(BigInteger.valueOf(2), "2");
		helper.testHex(BigInteger.valueOf(10), "A");
		helper.testHex(BigInteger.valueOf(15), "F");
		helper.testHex(BigInteger.valueOf(16), "10");
		helper.testHex(BigInteger.valueOf(31), "1F");
		helper.testHex(BigInteger.valueOf(32), "20");
		helper.testHex(BigInteger.valueOf(66), "42");
		helper.testHex(BigInteger.valueOf(419), "1A3");
	}

	@Test
	public void testQuotedDecimalZeros() {
		helper.testQuoted(BigInteger.ZERO, "2#0");
		helper.testQuoted(BigInteger.ZERO, "2#00");

		helper.testQuoted(BigInteger.ZERO, "3#0");
		helper.testQuoted(BigInteger.ZERO, "8#0");
	}

	@Test
	public void testQuotedDecimalOnes() {
		helper.testQuoted(BigInteger.ONE, "2#1");
		helper.testQuoted(BigInteger.ONE, "2#01");

		helper.testQuoted(BigInteger.ONE, "3#1");
		helper.testQuoted(BigInteger.ONE, "8#1");
	}

	@Test
	public void testQuotedDecimalBases() {
		helper.testQuoted(BigInteger.valueOf(2), "2#10");
		helper.testQuoted(BigInteger.valueOf(3), "3#10");
		helper.testQuoted(BigInteger.valueOf(4), "4#10");
		helper.testQuoted(BigInteger.valueOf(5), "5#10");
		helper.testQuoted(BigInteger.valueOf(10), "10#10");
		helper.testQuoted(BigInteger.valueOf(16), "16#10");

		helper.testQuoted(BigInteger.valueOf(100), "100#10");
	}

	@Test
	public void testQuotedSeparators() {
		helper.testQuoted(BigInteger.valueOf(2), "2#1_0");
		helper.testQuoted(BigInteger.valueOf(2), "2#1__0");
		helper.testQuoted(BigInteger.valueOf(4), "2#1_0_0");
	}

	@Test
	public void testQuotedDecimalTens() {
		helper.testQuoted(BigInteger.valueOf(10), "11#A");
		helper.testQuoted(BigInteger.valueOf(10), "12#A");
		helper.testQuoted(BigInteger.valueOf(10), "13#A");
		helper.testQuoted(BigInteger.valueOf(10), "14#A");
		helper.testQuoted(BigInteger.valueOf(10), "15#A");
		helper.testQuoted(BigInteger.valueOf(10), "16#A");
	}

	@Test
	public void testQuotedDecimalSingleLargeDigit() {
		helper.testQuoted(BigInteger.valueOf(10), "11#'10'");
		helper.testQuoted(BigInteger.valueOf(10), "12#'10'");
		helper.testQuoted(BigInteger.valueOf(10), "13#'10'");
		helper.testQuoted(BigInteger.valueOf(10), "14#'10'");
		helper.testQuoted(BigInteger.valueOf(10), "15#'10'");
		helper.testQuoted(BigInteger.valueOf(10), "16#'10'");
	}

	@Test
	public void testQuotedDecimalMultipleLargeDigitsSingleQuotes() {
		helper.testQuoted(BigInteger.valueOf(9 * 11 + 10), "11#'9''10'");
		helper.testQuoted(BigInteger.valueOf((9 * 12 + 10) * 12 + 11), "12#'9''10''11'");
		helper.testQuoted(BigInteger.valueOf(((9 * 13 + 10) * 13 + 11) * 13 + 12), "13#'9''10''11''12'");
	}

	@Test
	public void testQuotedDecimalMultipleLargeDigitsDoubleQuotes() {
		helper.testQuoted(BigInteger.valueOf(9 * 11 + 10), "11#'9\"10'");
		helper.testQuoted(BigInteger.valueOf((9 * 12 + 10) * 12 + 11), "12#'9\"10\"11'");
		helper.testQuoted(BigInteger.valueOf(((9 * 13 + 10) * 13 + 11) * 13 + 12), "13#'9\"10\"11\"12'");
	}

	@Test
	public void testQuotedDecimalMixedDigits() {
		helper.testQuoted(BigInteger.valueOf(666), "16#29'10'");
		helper.testQuoted(BigInteger.valueOf((11 * 16 + 5) * 16 + 10), "16#'11'5'10'");
		helper.testQuoted(BigInteger.valueOf(16 * 16 * 16), "16#'1'0'0'0");
		helper.testQuoted(BigInteger.valueOf((10 * 16 + 11) * 16 + 12), "16#'10\"11'C");
	}
}
