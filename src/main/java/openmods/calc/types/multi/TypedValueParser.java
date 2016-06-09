package openmods.calc.types.multi;

import java.math.BigInteger;

import openmods.calc.IValueParser;
import openmods.calc.PositionalNotationParser;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;

import org.apache.commons.lang3.tuple.Pair;

public class TypedValueParser implements IValueParser<TypedValue> {

	private static final PositionalNotationParser<BigInteger, Double> PARSER = new PositionalNotationParser<BigInteger, Double>() {
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
		protected Accumulator<Double> createFractionalAccumulator(int radix) {
			final double inverseRadix = 1.0 / radix;
			return new Accumulator<Double>() {
				private double value = 0;
				private double weight = inverseRadix;

				@Override
				public void add(int digit) {
					value += digit * weight;
					weight *= inverseRadix;
				}

				@Override
				public Double get() {
					return value;
				}
			};
		}
	};

	private final TypeDomain domain;

	public TypedValueParser(TypeDomain domain) {
		this.domain = domain;
	}

	@Override
	public TypedValue parseToken(Token token) {
		if (token.type == TokenType.STRING) return domain.create(String.class, token.value);

		final Pair<BigInteger, Double> result = PARSER.parseToken(token);
		final BigInteger intPart = result.getLeft();
		final Double fractionPart = result.getRight();

		if (fractionPart == null) return domain.create(BigInteger.class, intPart);

		final double total = intPart.doubleValue() + fractionPart;
		return domain.create(Double.class, total);
	}
}
