package openmods.calc;

import org.apache.commons.lang3.math.Fraction;

public class FractionParser implements IValueParser<Fraction> {

	private static final IntegerParser<Integer> INT_PARSER = new IntegerParser<Integer>() {
		@Override
		public Accumulator<Integer> createAccumulator(final int radix) {
			return new Accumulator<Integer>() {
				private int value = 0;

				@Override
				public void add(int digit) {
					value = value * radix + digit;
				}

				@Override
				public Integer get() {
					return value;
				}
			};
		}
	};

	@Override
	public Fraction parseToken(Token token) {
		if (token.type == TokenType.FLOAT_NUMBER) {
			final double value = Double.parseDouble(token.value);
			return Fraction.getFraction(value);
		} else {
			final int value = INT_PARSER.parseToken(token);
			return Fraction.getFraction(value, 1);
		}
	}

}
