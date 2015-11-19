package openmods.calc;

import java.math.BigInteger;

public class BigIntValueParser implements IValueParser<BigInteger> {

	private static final IntegerParser<BigInteger> INT_PARSER = new IntegerParser<BigInteger>() {
		@Override
		public Accumulator<BigInteger> createAccumulator(int radix) {
			final BigInteger bigRadix = BigInteger.valueOf(radix);
			return new Accumulator<BigInteger>() {
				private BigInteger value = BigInteger.ZERO;

				@Override
				public void add(int digit) {
					value = value.multiply(bigRadix).add(BigInteger.valueOf(digit));
				}

				@Override
				public BigInteger get() {
					return value;
				}
			};
		}
	};

	@Override
	public BigInteger parseToken(Token token) {
		if (token.type == TokenType.FLOAT_NUMBER) throw new NumberFormatException("Floats are not supported in this mode");
		return INT_PARSER.parseToken(token);
	}

}
