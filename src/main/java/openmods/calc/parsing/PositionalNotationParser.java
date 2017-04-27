package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.Iterator;
import openmods.calc.parsing.token.Token;
import org.apache.commons.lang3.tuple.Pair;

public abstract class PositionalNotationParser<I, F> {

	private static final Splitter DOT_SPLITTER = Splitter.on('.');

	public interface Accumulator<E> {
		public void add(int digit);

		public E get();
	}

	protected abstract Accumulator<I> createIntegerAccumulator(int radix);

	protected abstract Accumulator<F> createFractionalAccumulator(int radix);

	private interface PartParser {
		public <E> E parsePart(Accumulator<E> accumulator, String input, int radix);
	}

	private static final PartParser SIMPLE_DIGIT_PART_PARSER = new PartParser() {
		@Override
		public <E> E parsePart(Accumulator<E> accumulator, String input, int radix) {
			final char[] charArray = input.toCharArray();

			for (char ch : charArray) {
				if (ch == '_') continue;

				final int digit = Character.digit(ch, radix);
				if (digit < 0) throw invalidDigit(input, radix, ch);
				accumulator.add(digit);
			}

			return accumulator.get();
		}

	};

	private static final PartParser QUOTED_DIGIT_PART_PARSER = new PartParser() {
		@Override
		public <E> E parsePart(Accumulator<E> accumulator, String input, int radix) {
			String reminder = input;

			final int digitRange = Math.min(Character.MAX_RADIX, radix);
			while (!reminder.isEmpty()) {
				final char ch = reminder.charAt(0);
				reminder = reminder.substring(1);

				final int digit;

				if (ch == '_') continue;

				if (ch == '\'') {
					final int nextQuote = reminder.indexOf('\'');
					if (nextQuote < 0) throw new NumberFormatException("Unmatched quote in " + input);
					final String digitStr = reminder.substring(0, nextQuote);
					digit = Integer.parseInt(digitStr);
					if (digit >= radix) throw invalidDigit(input, radix, digitStr);
					reminder = reminder.substring(nextQuote + 1);
				} else {
					digit = Character.digit(ch, digitRange);
					if (digit < 0) throw invalidDigit(input, radix, ch);
				}

				accumulator.add(digit);
			}

			return accumulator.get();
		}
	};

	private Pair<I, F> parseNumber(PartParser partParser, String value, int radix) {
		final Iterator<String> parts = DOT_SPLITTER.split(value).iterator();
		Preconditions.checkState(parts.hasNext(), "Invalid input '%s'", value);

		final String integerPart = parts.next();
		final Accumulator<I> integerAccumulator = createIntegerAccumulator(radix);
		final I integer = partParser.parsePart(integerAccumulator, integerPart, radix);

		if (!parts.hasNext()) return Pair.of(integer, null);

		final String fractionalPart = parts.next();
		final Accumulator<F> fractionalAccumulator = createFractionalAccumulator(radix);
		final F fraction = partParser.parsePart(fractionalAccumulator, fractionalPart, radix);

		Preconditions.checkState(!parts.hasNext(), "More than one comman in '%s'", value);
		return Pair.of(integer, fraction);
	}

	private Pair<I, F> parseSimpleNumber(String value, int radix) {
		return parseNumber(SIMPLE_DIGIT_PART_PARSER, value, radix);
	}

	private Pair<I, F> parseQuotedNumber(String value, int radix) {
		return parseNumber(QUOTED_DIGIT_PART_PARSER, value.replace("\"", "''"), radix);
	}

	private Pair<I, F> parseQuotedNumber(String value) {
		final int radixEnd = value.indexOf('#');
		final String radixStr = value.substring(0, radixEnd);
		if (radixStr.isEmpty()) throw new NumberFormatException("No radix given");
		final int radix = Integer.parseInt(radixStr);
		if (radix < Character.MIN_RADIX) throw new NumberFormatException("Base must be larger than " + Character.MIN_RADIX);
		final String numberStr = value.substring(radixEnd + 1);
		return parseQuotedNumber(numberStr, radix);
	}

	public Pair<I, F> parseString(String value, int radix) {
		if (radix < Character.MIN_RADIX) throw new NumberFormatException("Base must be larger than " + Character.MIN_RADIX);
		return parseQuotedNumber(value, radix);
	}

	public Pair<I, F> parseToken(Token token) {
		switch (token.type) {
			case BIN_NUMBER:
				return parseSimpleNumber(token.value, 2);
			case OCT_NUMBER:
				return parseSimpleNumber(token.value, 8);
			case DEC_NUMBER:
				return parseSimpleNumber(token.value, 10);
			case HEX_NUMBER:
				return parseSimpleNumber(token.value, 16);
			case QUOTED_NUMBER:
				return parseQuotedNumber(token.value);
			default:
				throw new InvalidTokenException(token);
		}
	}

	private static NumberFormatException invalidDigit(String value, int radix, Object ch) {
		return new NumberFormatException(String.format("Number '%s' in base %d contains invalid digit '%s'", value, radix, ch));
	}
}
