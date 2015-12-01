package openmods.calc;

public class DoubleCalculator extends Calculator<Double> {

	public DoubleCalculator() {
		super(new DoubleParser(), Constant.create(0.0));
	}

	@Override
	protected void setupOperators(OperatorDictionary<Double> operators) {
		operators.registerUnaryOperator("neg", new UnaryOperator<Double>(1) {
			@Override
			protected Double execute(Double value) {
				return -value;
			}
		});

		operators.registerMixedOperator("+", new BinaryOperator<Double>(2) {
			@Override
			protected Double execute(Double left, Double right) {
				return left + right;
			}
		}, new UnaryOperator<Double>(1) {
			@Override
			protected Double execute(Double value) {
				return +value;
			}
		});

		operators.registerMixedOperator("-", new BinaryOperator<Double>(2) {
			@Override
			protected Double execute(Double left, Double right) {
				return left - right;
			}
		}, new UnaryOperator<Double>(1) {
			@Override
			protected Double execute(Double value) {
				return -value;
			}
		});

		operators.registerBinaryOperator("e", new BinaryOperator<Double>(2) {
			@Override
			protected Double execute(Double left, Double right) {
				return left * Math.pow(10, right);
			}
		});

		operators.registerBinaryOperator("*", new BinaryOperator<Double>(3) {
			@Override
			protected Double execute(Double left, Double right) {
				return left * right;
			}
		});

		operators.registerBinaryOperator("/", new BinaryOperator<Double>(3) {
			@Override
			protected Double execute(Double left, Double right) {
				return left / right;
			}
		});

		operators.registerBinaryOperator("^", new BinaryOperator<Double>(4) {
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

		globals.setSymbol("abs", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.abs(value);
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

		globals.setSymbol("min", new BinaryFunction<Double>() {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.min(left, right);
			}
		});

		globals.setSymbol("max", new BinaryFunction<Double>() {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.max(left, right);
			}
		});

	}

}
