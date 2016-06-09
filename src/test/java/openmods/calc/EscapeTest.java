package openmods.calc;

import gnu.trove.set.hash.TCharHashSet;
import openmods.calc.parsing.StringEscaper;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class EscapeTest {

	private static void verifyParse(String input, String output, int start, int end) {
		final Pair<String, Integer> result = StringEscaper.unescapeDelimitedString(input, start);
		Assert.assertEquals(output, result.getLeft());
		Assert.assertEquals(end, result.getRight().intValue());
	}

	private static void verifyFullParse(String input, String output) {
		verifyParse(input, output, 0, input.length());
	}

	@Test
	public void testSimpleStrings() {
		verifyFullParse("''", "");
		verifyFullParse("'abc'", "abc");

		verifyFullParse("\"\"", "");
		verifyFullParse("\"abc\"", "abc");

		verifyFullParse("'\"\"'", "\"\"");
		verifyFullParse("\"''\"", "''");

		verifyFullParse("'a\"bc'", "a\"bc");
		verifyFullParse("\"a'bc\"", "a'bc");
	}

	@Test
	public void testPartialStringEscape() {
		verifyParse("aaa'abc'", "abc", 3, 8);
		verifyParse("'abc'def", "abc", 0, 5);
		verifyParse("aaa'abc'ccc", "abc", 3, 8);
	}

	@Test
	public void testFullStringEscape() {
		verifyFullParse("'\\\\'", "\\");
		verifyFullParse("'\\\''", "'");
		verifyFullParse("\"\\\"\"", "\"");

		verifyFullParse("'\\r'", "\r");
		verifyFullParse("'\\n'", "\n");
		verifyFullParse("'\\b'", "\b");
		verifyFullParse("'\\t'", "\t");
		verifyFullParse("'\\f'", "\f");
		verifyFullParse("'\\0'", "\0");

		verifyFullParse("'\\x20'", " ");
		verifyFullParse("'a\\x20'", "a ");
		verifyFullParse("'\\x20a'", " a");
		verifyFullParse("'\\x00'", "\0");

		verifyFullParse("'\\u2603'", "\u2603");
		verifyFullParse("'\\uD83D\\uDE08'", "\uD83D\uDE08");
		verifyFullParse("'\\U0001F608'", "\uD83D\uDE08");
	}

	private static void verifyEscape(String input, String output) {
		Assert.assertEquals(output, StringEscaper.escapeString(input, '\'', new TCharHashSet(new char[] { '"' })));
	}

	@Test
	public void testEscape() {
		verifyEscape("a", "'a'");
		verifyEscape(" ", "' '");
		verifyEscape("  ", "'  '");

		verifyEscape("\r", "'\\r'");
		verifyEscape("\n", "'\\n'");
		verifyEscape("\b", "'\\b'");
		verifyEscape("\t", "'\\t'");
		verifyEscape("\f", "'\\f'");
		verifyEscape("\0", "'\\0'");

		verifyEscape(Character.toString((char)0x05), "'\\x05'");
		verifyEscape(Character.toString((char)0x15), "'\\x15'");

		verifyEscape("\u2603", "'\\u2603'");
		verifyEscape("\u0603", "'\\u0603'");

		verifyEscape("\uD83D\uDE08", "'\\U0001F608'");
	}
}
