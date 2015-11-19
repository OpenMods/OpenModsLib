package openmods.calc;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

public class BigIntValueParserTest {

	public final BigIntValueParser parser = new BigIntValueParser();

	private BigInteger parse(TokenType type, String value) {
		return parser.parseToken(new Token(type, value));
	}

	private BigInteger bin(String value) {
		return parse(TokenType.BIN_NUMBER, value);
	}

	private BigInteger oct(String value) {
		return parse(TokenType.OCT_NUMBER, value);
	}

	private BigInteger dec(String value) {
		return parse(TokenType.DEC_NUMBER, value);
	}

	private BigInteger hex(String value) {
		return parse(TokenType.HEX_NUMBER, value);
	}

	private BigInteger q(String value) {
		return parse(TokenType.QUOTED_NUMBER, value);
	}

	@Test
	public void testBinary() {
		Assert.assertEquals(BigInteger.ZERO, bin("0"));
		Assert.assertEquals(BigInteger.ONE, bin("1"));
		Assert.assertEquals(BigInteger.ZERO, bin("00"));
		Assert.assertEquals(BigInteger.ONE, bin("01"));
		Assert.assertEquals(BigInteger.valueOf(2), bin("10"));
		Assert.assertEquals(BigInteger.valueOf(3), bin("11"));
		Assert.assertEquals(BigInteger.valueOf(4), bin("100"));
		Assert.assertEquals(BigInteger.valueOf(7), bin("111"));
	}

	@Test
	public void testOctal() {
		Assert.assertEquals(BigInteger.ZERO, oct("0"));
		Assert.assertEquals(BigInteger.ZERO, oct("00"));
		Assert.assertEquals(BigInteger.ONE, oct("1"));
		Assert.assertEquals(BigInteger.ONE, oct("01"));
		Assert.assertEquals(BigInteger.valueOf(2), oct("2"));
		Assert.assertEquals(BigInteger.valueOf(7), oct("7"));
		Assert.assertEquals(BigInteger.valueOf(8), oct("10"));
		Assert.assertEquals(BigInteger.valueOf(15), oct("17"));
		Assert.assertEquals(BigInteger.valueOf(16), oct("20"));
		Assert.assertEquals(BigInteger.valueOf(34), oct("42"));
		Assert.assertEquals(BigInteger.valueOf(83), oct("123"));
	}

	@Test
	public void testDecimal() {
		Assert.assertEquals(BigInteger.ZERO, dec("0"));
		Assert.assertEquals(BigInteger.ZERO, dec("00"));
		Assert.assertEquals(BigInteger.ONE, dec("1"));
		Assert.assertEquals(BigInteger.ONE, dec("01"));
		Assert.assertEquals(BigInteger.valueOf(2), dec("2"));
		Assert.assertEquals(BigInteger.valueOf(9), dec("9"));
		Assert.assertEquals(BigInteger.valueOf(10), dec("10"));
		Assert.assertEquals(BigInteger.valueOf(19), dec("19"));
		Assert.assertEquals(BigInteger.valueOf(20), dec("20"));
		Assert.assertEquals(BigInteger.valueOf(42), dec("42"));
		Assert.assertEquals(BigInteger.valueOf(123), dec("123"));
	}

	@Test
	public void testHexadecimal() {
		Assert.assertEquals(BigInteger.ZERO, hex("0"));
		Assert.assertEquals(BigInteger.ZERO, hex("00"));
		Assert.assertEquals(BigInteger.ONE, hex("1"));
		Assert.assertEquals(BigInteger.ONE, hex("01"));
		Assert.assertEquals(BigInteger.valueOf(2), hex("2"));
		Assert.assertEquals(BigInteger.valueOf(10), hex("A"));
		Assert.assertEquals(BigInteger.valueOf(15), hex("F"));
		Assert.assertEquals(BigInteger.valueOf(16), hex("10"));
		Assert.assertEquals(BigInteger.valueOf(31), hex("1F"));
		Assert.assertEquals(BigInteger.valueOf(32), hex("20"));
		Assert.assertEquals(BigInteger.valueOf(66), hex("42"));
		Assert.assertEquals(BigInteger.valueOf(419), hex("1A3"));
	}

	@Test
	public void testQuotedDecimalZeros() {
		Assert.assertEquals(BigInteger.ZERO, q("2#0"));
		Assert.assertEquals(BigInteger.ZERO, q("2#00"));

		Assert.assertEquals(BigInteger.ZERO, q("3#0"));
		Assert.assertEquals(BigInteger.ZERO, q("8#0"));
	}

	@Test
	public void testQuotedDecimalOnes() {
		Assert.assertEquals(BigInteger.ONE, q("2#1"));
		Assert.assertEquals(BigInteger.ONE, q("2#01"));

		Assert.assertEquals(BigInteger.ONE, q("3#1"));
		Assert.assertEquals(BigInteger.ONE, q("8#1"));
	}

	@Test
	public void testQuotedDecimalBases() {
		Assert.assertEquals(BigInteger.valueOf(2), q("2#10"));
		Assert.assertEquals(BigInteger.valueOf(3), q("3#10"));
		Assert.assertEquals(BigInteger.valueOf(4), q("4#10"));
		Assert.assertEquals(BigInteger.valueOf(5), q("5#10"));
		Assert.assertEquals(BigInteger.valueOf(10), q("10#10"));
		Assert.assertEquals(BigInteger.valueOf(16), q("16#10"));
	}

	@Test
	public void testQuotedDecimalTens() {
		Assert.assertEquals(BigInteger.valueOf(10), q("11#A"));
		Assert.assertEquals(BigInteger.valueOf(10), q("12#A"));
		Assert.assertEquals(BigInteger.valueOf(10), q("13#A"));
		Assert.assertEquals(BigInteger.valueOf(10), q("14#A"));
		Assert.assertEquals(BigInteger.valueOf(10), q("15#A"));
		Assert.assertEquals(BigInteger.valueOf(10), q("16#A"));
	}

	@Test
	public void testQuotedDecimalSingleLargeDigit() {
		Assert.assertEquals(BigInteger.valueOf(10), q("11#'10'"));
		Assert.assertEquals(BigInteger.valueOf(10), q("12#'10'"));
		Assert.assertEquals(BigInteger.valueOf(10), q("13#'10'"));
		Assert.assertEquals(BigInteger.valueOf(10), q("14#'10'"));
		Assert.assertEquals(BigInteger.valueOf(10), q("15#'10'"));
		Assert.assertEquals(BigInteger.valueOf(10), q("16#'10'"));
	}

	@Test
	public void testQuotedDecimalMultipleLargeDigitsSingleQuotes() {
		Assert.assertEquals(BigInteger.valueOf(9 * 11 + 10), q("11#'9''10'"));
		Assert.assertEquals(BigInteger.valueOf((9 * 12 + 10) * 12 + 11), q("12#'9''10''11'"));
		Assert.assertEquals(BigInteger.valueOf(((9 * 13 + 10) * 13 + 11) * 13 + 12), q("13#'9''10''11''12'"));
	}

	@Test
	public void testQuotedDecimalMultipleLargeDigitsDoubleQuotes() {
		Assert.assertEquals(BigInteger.valueOf(9 * 11 + 10), q("11#'9\"10'"));
		Assert.assertEquals(BigInteger.valueOf((9 * 12 + 10) * 12 + 11), q("12#'9\"10\"11'"));
		Assert.assertEquals(BigInteger.valueOf(((9 * 13 + 10) * 13 + 11) * 13 + 12), q("13#'9\"10\"11\"12'"));
	}

	@Test
	public void testQuotedDecimalMixedDigits() {
		Assert.assertEquals(BigInteger.valueOf(666), q("16#29'10'"));
		Assert.assertEquals(BigInteger.valueOf((11 * 16 + 5) * 16 + 10), q("16#'11'5'10'"));
		Assert.assertEquals(BigInteger.valueOf(16 * 16 * 16), q("16#'1'0'0'0"));
		Assert.assertEquals(BigInteger.valueOf((10 * 16 + 11) * 16 + 12), q("16#'10\"11'C"));
	}
}
