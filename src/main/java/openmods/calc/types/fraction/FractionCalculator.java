package openmods.calc.types.fraction;

import openmods.calc.*;
import openmods.calc.parsing.ICompiler;
import openmods.calc.parsing.InfixCompiler;
import openmods.config.simpler.Configurable;

import org.apache.commons.lang3.math.Fraction;

import com.google.common.collect.Ordering;

public class FractionCalculator extends Calculator<Fraction> {

	private static final int MAX_PRIO = 5;
	@Configurable
	public boolean properFractions;

	@Configurable
	public boolean expand;

	public FractionCalculator() {
		super(new FractionParser(), Fraction.ZERO);
	}

	private static final BinaryOperator<Fraction> MULTIPLY = new BinaryOperator<Fraction>("*", MAX_PRIO - 3) {
		@Override
		protected Fraction execute(Fraction left, Fraction right) {
			return left.multiplyBy(right);
		}
	};

	@Override
	protected ICompiler<Fraction> createInfixCompiler(IValueParser<Fraction> valueParser, OperatorDictionary<Fraction> operators) {
		return new InfixCompiler<Fraction>(valueParser, operators, MULTIPLY);
	}

	@Override
	protected void setupOperators(OperatorDictionary<Fraction> operators) {
		operators.registerUnaryOperator(new UnaryOperator<Fraction>("neg") {
			@Override
			protected Fraction execute(Fraction value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("+", MAX_PRIO - 4) {
			@Override
			protected Fraction execute(Fraction left, Fraction right) {
				return left.add(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<Fraction>("+") {
			@Override
			protected Fraction execute(Fraction value) {
				return value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("-", MAX_PRIO - 4) {
			@Override
			protected Fraction execute(Fraction left, Fraction right) {
				return left.subtract(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<Fraction>("-") {
			@Override
			protected Fraction execute(Fraction value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(MULTIPLY);

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("/", MAX_PRIO - 3) {
			@Override
			protected Fraction execute(Fraction left, Fraction right) {
				return left.divideBy(right);
			}
		});
	}

	private static Fraction createFraction(int value) {
		return Fraction.getFraction(value, 1);
	}

	@Override
	protected void setupGlobals(TopFrame<Fraction> globals) {
		globals.setSymbol("abs", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return value.abs();
			}
		});

		globals.setSymbol("sgn", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(Integer.signum(value.getNumerator()));
			}
		});

		globals.setSymbol("numerator", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(value.getNumerator());
			}
		});

		globals.setSymbol("denominator", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(value.getDenominator());
			}
		});

		globals.setSymbol("frac", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return Fraction.getFraction(value.getProperNumerator(), value.getDenominator());
			}
		});

		globals.setSymbol("int", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(value.getProperWhole());
			}
		});

		globals.setSymbol("sqrt", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return Fraction.getFraction(Math.sqrt(value.doubleValue()));
			}
		});

		globals.setSymbol("log", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return Fraction.getFraction(Math.log(value.doubleValue()));
			}
		});

		globals.setSymbol("min", new AccumulatorFunction() {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return Ordering.natural().min(result, value);
			}
		});

		globals.setSymbol("max", new AccumulatorFunction() {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return Ordering.natural().max(result, value);
			}
		});

	}

	@Override
	public String toString(Fraction value) {
		if (expand) return Double.toString(value.doubleValue());
		return properFractions? value.toProperString() : value.toString();
	}
}
