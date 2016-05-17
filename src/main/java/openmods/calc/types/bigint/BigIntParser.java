package openmods.calc.types.bigint;

import java.math.BigInteger;

import openmods.calc.IValueParser;
import openmods.calc.PositionalNotationParser;
import openmods.calc.parsing.Token;

import org.apache.commons.lang3.tuple.Pair;

public class BigIntParser implements IValueParser<BigInteger> {

	private static final PositionalNotationParser<BigInteger> PARSER = new PositionalNotationParser<BigInteger>() {
		@Override
		public Accumulator<BigInteger> createIntegerAccumulator(int radix) {
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

		@Override
		public Accumulator<BigInteger> createFractionalAccumulator(int radix) {
			throw new IllegalArgumentException("Fractional part not allowed");
		}
	};

	@Override
	public BigInteger parseToken(Token token) {
		final Pair<BigInteger, BigInteger> result = PARSER.parseToken(token);
		return result.getLeft();
	}

}
