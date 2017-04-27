package openmods.calc.types.multi;

import java.math.BigInteger;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.PositionalNotationParser;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import org.apache.commons.lang3.tuple.Pair;

public class TypedValueParser implements IValueParser<TypedValue> {

	public static final PositionalNotationParser<BigInteger, Double> NUMBER_PARSER = new PositionalNotationParser<BigInteger, Double>() {
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

	public static TypedValue mergeNumberParts(TypeDomain domain, Pair<BigInteger, Double> result) {
		final BigInteger intPart = result.getLeft();
		final Double fractionPart = result.getRight();

		if (fractionPart == null) return domain.create(BigInteger.class, intPart);

		final double total = intPart.doubleValue() + fractionPart;
		return domain.create(Double.class, total);
	}

	private final TypeDomain domain;

	public TypedValueParser(TypeDomain domain) {
		this.domain = domain;
	}

	@Override
	public TypedValue parseToken(Token token) {
		if (token.type == TokenType.STRING) return domain.create(String.class, token.value);

		final Pair<BigInteger, Double> result = NUMBER_PARSER.parseToken(token);
		return mergeNumberParts(domain, result);
	}
}
