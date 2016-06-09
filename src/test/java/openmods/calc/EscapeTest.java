package openmods.calc;

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

}
