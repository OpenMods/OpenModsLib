package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import gnu.trove.set.TCharSet;
import org.apache.commons.lang3.tuple.Pair;

public class StringEscaper {

	public static final BiMap<Character, Character> ESCAPES = ImmutableBiMap.<Character, Character> builder()
			.put('\\', '\\')
			.put('\'', '\'')
			.put('"', '"')
			.put('0', '\0')
			.put('b', '\b')
			.put('t', '\t')
			.put('n', '\n')
			.put('f', '\f')
			.put('r', '\r')
			.build();

	public static String escapeString(String value, char delimiter, TCharSet nonEscapeChars) {
		final StringBuilder builder = new StringBuilder();
		builder.append(delimiter);

		int pos = 0;
		while (pos < value.length()) {
			final char ch = value.charAt(pos++);
			if (nonEscapeChars.contains(ch)) {
				builder.append(ch);
			} else {
				final Character possibleEscape = ESCAPES.inverse().get(ch);
				if (possibleEscape != null) {
					builder.append("\\").append(possibleEscape);
				} else if (ch >= 0x20 && ch < 0x7F) {
					builder.append(ch);
				} else if (ch <= 0xFF) {
					builder.append(String.format("\\x%02X", (int)ch));
				} else if (!Character.isHighSurrogate(ch)) {
					builder.append(String.format("\\u%04X", (int)ch));
				} else {
					Preconditions.checkState(pos < value.length(), "Malformed UTF-16 string: high surrogate at end of string");
					final char nextCh = value.charAt(pos++);
					Preconditions.checkState(Character.isLowSurrogate(nextCh), "Malformed UTF-16 string: expected low surrogate, got: %s", Integer.toHexString(nextCh));
					final int codePoint = Character.toCodePoint(ch, nextCh);
					builder.append(String.format("\\U%08X", codePoint));
				}
			}

		}

		builder.append(delimiter);
		return builder.toString();
	}

	public static Pair<String, Integer> unescapeDelimitedString(String input, int start) {
		final StringBuilder result = new StringBuilder();

		int pos = start;
		final char delimiter = input.charAt(pos++);

		while (true) {
			if (pos >= input.length()) throw new IllegalArgumentException("Unterminated string: '" + result + "'");
			final char ch = input.charAt(pos++);
			if (ch == delimiter) break;
			if (ch == '\\') {
				if (pos >= input.length()) throw new IllegalArgumentException("Unterminated escape sequence: '" + result + "'");
				final char escaped = input.charAt(pos++);
				switch (escaped) {
					case 'x':
						result.append(parseHexChar(input, pos, 2));
						pos += 2;
						break;
					case 'u':
						result.append(parseHexChar(input, pos, 4));
						pos += 4;
						break;
					case 'U':
						result.append(parseHexChar(input, pos, 8));
						pos += 8;
						break;
					// TODO: case 'N':
					default: {
						final Character sub = ESCAPES.get(escaped);
						Preconditions.checkArgument(sub != null, "Invalid escape sequence: " + escaped);
						result.append(sub);
					}
				}
			} else {
				result.append(ch);
			}
		}

		return Pair.of(result.toString(), pos);
	}

	private static char[] parseHexChar(String input, int pos, int digits) {
		final String code = input.substring(pos, pos + digits);
		final int intCode = Integer.parseInt(code, 16);
		return Character.toChars(intCode);
	}

}
