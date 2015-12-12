package openmods.calc;

import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public abstract class PositionalNotationParser<E> {

	private final Splitter dotSplitter = Splitter.on('.');

	public interface Accumulator<E> {
		public void add(int digit);

		public E get();
	}

	protected abstract Accumulator<E> createIntegerAccumulator(int radix);

	protected abstract Accumulator<E> createFractionalAccumulator(int radix);

	private E parsePart(Accumulator<E> accumulator, String input, int radix) {
		final char[] charArray = input.toCharArray();
		// if (reverse) ArrayUtils.reverse(charArray);

		for (char ch : charArray) {
			final int digit = Character.digit(ch, radix);
			if (digit < 0) throw invalidDigit(input, radix, ch);
			accumulator.add(digit);
		}

		return accumulator.get();
	}

	private Pair<E, E> parseFixedBaseNumber(String value, int radix) {
		if (radix < Character.MIN_RADIX) throw new NumberFormatException("Base must be larger than " + Character.MIN_RADIX);
		if (radix > Character.MAX_RADIX) throw new NumberFormatException("Base must be smaller than " + Character.MAX_RADIX);

		final Iterator<String> parts = dotSplitter.split(value).iterator();
		Preconditions.checkState(parts.hasNext(), "Invalid input '%s'", value);

		final String integerPart = parts.next();
		final Accumulator<E> integerAccumulator = createIntegerAccumulator(radix);
		final E integer = parsePart(integerAccumulator, integerPart, radix);

		if (!parts.hasNext()) return Pair.of(integer, null);

		final String fractionalPart = parts.next();
		final Accumulator<E> fractionalAccumulator = createFractionalAccumulator(radix);
		final E fraction = parsePart(fractionalAccumulator, fractionalPart, radix);

		Preconditions.checkState(!parts.hasNext(), "More than one comman in '%s'", value);
		return Pair.of(integer, fraction);
	}

	private E parseQuotedPart(Accumulator<E> accumulator, final String input, int radix) {
		if (radix < Character.MIN_RADIX) throw new NumberFormatException("Base must be larger than " + Character.MIN_RADIX);

		String reminder = input;
		// if (reverse) StringUtils.reverse(reminder);

		while (!reminder.isEmpty()) {
			final char ch = reminder.charAt(0);
			reminder = reminder.substring(1);

			final int digit;

			if (ch == '\'') {
				final int nextQuote = reminder.indexOf('\'');
				if (nextQuote < 0) throw new NumberFormatException("Unmatched quote in " + input);
				final String digitStr = reminder.substring(0, nextQuote);
				digit = Integer.parseInt(digitStr);
				if (digit >= radix) throw invalidDigit(input, radix, digitStr);
				reminder = reminder.substring(nextQuote + 1);
			} else {
				digit = Character.digit(ch, radix);
				if (digit < 0) throw invalidDigit(input, radix, ch);
			}

			accumulator.add(digit);
		}

		return accumulator.get();
	}

	protected Pair<E, E> parseQuotedNumber(String value) {
		final int radixEnd = value.indexOf('#');
		final String radixStr = value.substring(0, radixEnd);
		if (radixStr.isEmpty()) throw new NumberFormatException("No radix given");
		final int radix = Integer.parseUnsignedInt(radixStr);
		final String numberStr = value.substring(radixEnd + 1).replace("\"", "''");

		final Iterator<String> parts = dotSplitter.split(numberStr).iterator();
		Preconditions.checkState(parts.hasNext(), "Invalid input '%s'", value);

		final String integerPart = parts.next();
		final Accumulator<E> integerAccumulator = createIntegerAccumulator(radix);
		final E integer = parseQuotedPart(integerAccumulator, integerPart, radix);

		if (!parts.hasNext()) return Pair.of(integer, null);

		final String fractionalPart = parts.next();
		final Accumulator<E> fractionalAccumulator = createFractionalAccumulator(radix);
		final E fraction = parseQuotedPart(fractionalAccumulator, fractionalPart, radix);

		Preconditions.checkState(!parts.hasNext(), "More than one comman in '%s'", value);
		return Pair.of(integer, fraction);
	}

	public Pair<E, E> parseToken(Token token) {
		switch (token.type) {
			case BIN_NUMBER:
				return parseFixedBaseNumber(token.value, 2);
			case OCT_NUMBER:
				return parseFixedBaseNumber(token.value, 8);
			case DEC_NUMBER:
				return parseFixedBaseNumber(token.value, 10);
			case HEX_NUMBER:
				return parseFixedBaseNumber(token.value, 16);
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
