package openmods.calc.types.fp;

import openmods.calc.*;
import openmods.config.simpler.Configurable;

public class DoubleCalculator extends Calculator<Double> {

	private static final int MAX_PRIO = 5;

	@Configurable
	public int base = 10;

	@Configurable
	public boolean allowStandardPrinter = false;

	@Configurable
	public boolean uniformBaseNotation = false;

	private final DoublePrinter printer = new DoublePrinter(8);

	public DoubleCalculator() {
		super(new DoubleParser(), 0.0);
	}

	@Override
	protected void setupOperators(OperatorDictionary<Double> operators) {
		operators.registerUnaryOperator("neg", new UnaryOperator<Double>() {
			@Override
			protected Double execute(Double value) {
				return -value;
			}
		});

		operators.registerMixedOperator("+", new BinaryOperator<Double>(MAX_PRIO - 4) {
			@Override
			protected Double execute(Double left, Double right) {
				return left + right;
			}
		}, new UnaryOperator<Double>() {
			@Override
			protected Double execute(Double value) {
				return +value;
			}
		});

		operators.registerMixedOperator("-", new BinaryOperator<Double>(MAX_PRIO - 4) {
			@Override
			protected Double execute(Double left, Double right) {
				return left - right;
			}
		}, new UnaryOperator<Double>() {
			@Override
			protected Double execute(Double value) {
				return -value;
			}
		});

		operators.registerBinaryOperator("*", new BinaryOperator<Double>(MAX_PRIO - 3) {
			@Override
			protected Double execute(Double left, Double right) {
				return left * right;
			}
		});

		operators.registerBinaryOperator("/", new BinaryOperator<Double>(MAX_PRIO - 3) {
			@Override
			protected Double execute(Double left, Double right) {
				return left / right;
			}
		});

		operators.registerBinaryOperator("%", new BinaryOperator<Double>(MAX_PRIO - 3) {
			@Override
			protected Double execute(Double left, Double right) {
				return left % right;
			}
		});

		operators.registerBinaryOperator("^", new BinaryOperator<Double>(MAX_PRIO - 2) {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.pow(left, right);
			}
		});
	}

	@Override
	protected void setupGlobals(TopFrame<Double> globals) {
		globals.setSymbol("PI", Constant.create(Math.PI));
		globals.setSymbol("E", Constant.create(Math.E));
		globals.setSymbol("INF", Constant.create(Double.POSITIVE_INFINITY));
		globals.setSymbol("MAX", Constant.create(Double.MIN_VALUE));

		globals.setSymbol("abs", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.abs(value);
			}
		});

		globals.setSymbol("sgn", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.signum(value);
			}
		});

		globals.setSymbol("sqrt", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.sqrt(value);
			}
		});

		globals.setSymbol("ceil", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.ceil(value);
			}
		});

		globals.setSymbol("floor", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.floor(value);
			}
		});

		globals.setSymbol("cos", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.cos(value);
			}
		});

		globals.setSymbol("sin", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.sin(value);
			}
		});

		globals.setSymbol("tan", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.tan(value);
			}
		});

		globals.setSymbol("acos", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.acos(value);
			}
		});

		globals.setSymbol("asin", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.asin(value);
			}
		});

		globals.setSymbol("atan", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.atan(value);
			}
		});

		globals.setSymbol("atan2", new BinaryFunction<Double>() {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.atan2(left, right);
			}

		});

		globals.setSymbol("log10", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.log10(value);
			}
		});

		globals.setSymbol("ln", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.log(value);
			}
		});

		globals.setSymbol("log", new BinaryFunction<Double>() {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.log(left) / Math.log(right);
			}
		});

		globals.setSymbol("exp", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.exp(value);
			}
		});

		globals.setSymbol("min", new AccumulatorFunction() {
			@Override
			protected Double accumulate(Double result, Double value) {
				return Math.min(result, value);
			}
		});

		globals.setSymbol("max", new AccumulatorFunction() {
			@Override
			protected Double accumulate(Double result, Double value) {
				return Math.max(result, value);
			}
		});

		globals.setSymbol("rad", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.toRadians(value);
			}
		});

		globals.setSymbol("deg", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.toDegrees(value);
			}
		});

	}

	@Override
	public String toString(Double value) {
		if (base == 10 && !allowStandardPrinter && !uniformBaseNotation) {
			return value.toString();
		} else {
			if (value.isNaN()) return "NaN";
			if (value.isInfinite()) return value > 0? "+Inf" : "-Inf";
			final String result = printer.toString(value, base);
			return decorateBase(!uniformBaseNotation, base, result);
		}
	}

}
