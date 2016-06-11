package openmods.calc.types.fp;

import openmods.calc.PositionalNotationPrinter;
import org.apache.commons.lang3.tuple.Pair;

public class DoublePrinter extends PositionalNotationPrinter<Double> {

	public DoublePrinter(int maxDigits) {
		super(maxDigits);
	}

	@Override
	protected Pair<Double, Double> splitNumber(Double value) {
		final double integer = value.intValue();
		final double fractional = value - integer;
		return Pair.of(integer, fractional);
	}

	@Override
	protected IDigitProvider createIntegerDigitProvider(final Double value, final int radix) {
		return new IDigitProvider() {
			// should already be int
			private int remainder = value.intValue();

			@Override
			public int getNextDigit() {
				final int digit = remainder % radix;
				remainder /= radix;
				return digit;
			}

			@Override
			public boolean hasNextDigit() {
				return remainder > 0;
			}
		};
	}

	@Override
	protected IDigitProvider createFractionalDigitProvider(final Double value, int radix) {
		final double doubleRadix = radix;
		return new IDigitProvider() {
			private double remainder = value;

			@Override
			public int getNextDigit() {
				// very naive (read: stupid) algorithm
				remainder *= doubleRadix;
				int result = (int)remainder;
				remainder %= 1;
				return result;
			}

			@Override
			public boolean hasNextDigit() {
				return remainder > 0;
			}
		};
	}

	@Override
	protected boolean isNegative(Double value) {
		return value < 0;
	}

	@Override
	protected Double negate(Double value) {
		return -value;
	}

	@Override
	protected boolean isZero(Double value) {
		return value == 0;
	}
}