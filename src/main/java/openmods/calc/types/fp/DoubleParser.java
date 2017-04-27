package openmods.calc.types.fp;

import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.PositionalNotationParser;
import openmods.calc.parsing.token.Token;
import org.apache.commons.lang3.tuple.Pair;

public class DoubleParser implements IValueParser<Double> {

	private static final PositionalNotationParser<Double, Double> PARSER = new PositionalNotationParser<Double, Double>() {
		@Override
		public Accumulator<Double> createIntegerAccumulator(int radix) {
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

	@Override
	public Double parseToken(Token token) {
		final Pair<Double, Double> result = PARSER.parseToken(token);
		final Double left = result.getLeft();
		final Double right = result.getRight();

		return right != null? left + right : left;
	}
}
