package openmods.calc;

import org.apache.commons.lang3.tuple.Pair;

public abstract class PositionalNotationPrinter<E> {

	private final int maxDigits;

	public PositionalNotationPrinter(int maxDigits) {
		this.maxDigits = maxDigits;
	}

	public interface IDigitProvider {
		public int getNextDigit();

		public boolean hasNextDigit();
	}

	protected abstract IDigitProvider createIntegerDigitProvider(E value, int radix);

	protected abstract IDigitProvider createFractionalDigitProvider(E value, int radix);

	protected abstract boolean isNegative(E value);

	protected abstract boolean isZero(E value);

	protected abstract E negate(E value);

	protected abstract Pair<E, E> splitNumber(E value);

	private static final char[] digits = {
			'0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b',
			'c', 'd', 'e', 'f', 'g', 'h',
			'i', 'j', 'k', 'l', 'm', 'n',
			'o', 'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'x', 'y', 'z'
	};

	private static String digitToString(int digit) {
		if (digit >= digits.length) {
			return "'" + digit + "'";
		} else {
			return String.valueOf(digits[digit]);
		}
	}

	public String toString(E value, int radix) {
		final StringBuilder result = new StringBuilder();

		final boolean isNegative = isNegative(value);
		if (isNegative) value = negate(value);

		final Pair<E, E> split = splitNumber(value);

		final E integer = split.getLeft();

		if (integer != null) {
			if (isZero(integer)) {
				result.append('0');
			} else {
				final IDigitProvider provider = createIntegerDigitProvider(integer, radix);

				while (provider.hasNextDigit()) {
					final int digit = provider.getNextDigit();
					result.insert(0, digitToString(digit));
				}
			}

		}

		if (isNegative) result.insert(0, "-");

		int digitCount = 0;
		final E fractional = split.getRight();
		if (fractional != null && !isZero(fractional)) {
			result.append('.');
			final IDigitProvider provider = createFractionalDigitProvider(fractional, radix);

			while (provider.hasNextDigit() && digitCount++ < maxDigits) {
				final int digit = provider.getNextDigit();
				result.append(digitToString(digit));
			}
		}

		return result.toString().replace("''", "\"");
	}
}
