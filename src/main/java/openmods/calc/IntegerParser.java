package openmods.calc;

public abstract class IntegerParser<E> {

	public interface Accumulator<E> {
		public void add(int digit);

		public E get();
	}

	protected abstract Accumulator<E> createAccumulator(int radix);

	private E parseInteger(String value, int radix) {
		if (radix < Character.MIN_RADIX) throw new NumberFormatException("Base must be larger than " + Character.MIN_RADIX);
		if (radix > Character.MAX_RADIX) throw new NumberFormatException("Base must be smaller than " + Character.MAX_RADIX);

		final Accumulator<E> accumulator = createAccumulator(radix);

		for (char ch : value.toCharArray()) {
			final int digit = Character.digit(ch, radix);
			if (digit < 0) throw invalidDigit(value, radix, ch);
			accumulator.add(digit);
		}

		return accumulator.get();
	}

	private E parseQuotedInteger(final String input, int radix) {
		if (radix < Character.MIN_RADIX) throw new NumberFormatException("Base must be larger than " + Character.MIN_RADIX);

		final Accumulator<E> accumulator = createAccumulator(radix);

		String reminder = input;

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

	protected E parseQuotedInteger(String value) {
		final int radixEnd = value.indexOf('#');
		final String radixStr = value.substring(0, radixEnd);
		if (radixStr.isEmpty()) throw new NumberFormatException("No radix given");
		final int base = Integer.parseUnsignedInt(radixStr);
		final String numberStr = value.substring(radixEnd + 1).replace("\"", "''");
		return parseQuotedInteger(numberStr, base);
	}

	public E parseToken(Token token) {
		switch (token.type) {
			case BIN_NUMBER:
				return parseInteger(token.value, 2);
			case OCT_NUMBER:
				return parseInteger(token.value, 8);
			case DEC_NUMBER:
				return parseInteger(token.value, 10);
			case HEX_NUMBER:
				return parseInteger(token.value, 16);
			case QUOTED_NUMBER:
				return parseQuotedInteger(token.value);
			default:
				throw new InvalidTokenException(token);
		}
	}

	private static NumberFormatException invalidDigit(String value, int radix, Object ch) {
		return new NumberFormatException(String.format("Number '%s' in base %d contains invalid digit '%s'", value, radix, ch));
	}
}
