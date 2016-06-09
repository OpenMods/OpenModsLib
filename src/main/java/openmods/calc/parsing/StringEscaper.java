package openmods.calc.parsing;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

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
