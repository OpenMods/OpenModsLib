package openmods.calc;

public class DoubleParser implements IValueParser<Double> {

	private static final IntegerParser<Double> INT_PARSER = new IntegerParser<Double>() {
		@Override
		public Accumulator<Double> createAccumulator(int radix) {
			final double doubleRadix = radix;
			return new Accumulator<Double>() {
				private double value = 0;

				@Override
				public void add(int digit) {
					value = value * doubleRadix + digit;
				}

				@Override
				public Double get() {
					return value;
				}
			};
		}
	};

	@Override
	public Double parseToken(Token token) {
		if (token.type == TokenType.FLOAT_NUMBER) return Double.parseDouble(token.value);
		return INT_PARSER.parseToken(token);
	}

}
