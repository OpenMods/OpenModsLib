package openmods.calc.types.fp;

import java.util.Map;
import openmods.calc.BasicCalculatorFactory;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.Constant;
import openmods.calc.ExprType;
import openmods.calc.GenericFunctions;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.ICalculatorFactory;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.config.simpler.Configurable;

public class DoubleCalculator<M> extends Calculator<Double, M> {

	private static final double NULL_VALUE = 0.0;

	@Configurable
	public int base = 10;

	@Configurable
	public boolean allowStandardPrinter = false;

	@Configurable
	public boolean uniformBaseNotation = false;

	private final DoublePrinter printer = new DoublePrinter(8);

	public DoubleCalculator(Map<M, ICompiler<Double>> compilers) {
		super(NULL_VALUE, compilers);
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

	private static final int MAX_PRIO = 5;

	public static <E> Calculator<Double, E> create(ICalculatorFactory<Double, E, ? extends Calculator<Double, E>> factory) {
		final OperatorDictionary<Double> operators = new OperatorDictionary<Double>();

		operators.registerUnaryOperator(new UnaryOperator<Double>("neg") {
			@Override
			public Double execute(Double value) {
				return -value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Double>("+", MAX_PRIO - 4) {
			@Override
			public Double execute(Double left, Double right) {
				return left + right;
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<Double>("+") {
			@Override
			public Double execute(Double value) {
				return +value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Double>("-", MAX_PRIO - 4) {
			@Override
			public Double execute(Double left, Double right) {
				return left - right;
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<Double>("-") {
			@Override
			public Double execute(Double value) {
				return -value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Double>("*", MAX_PRIO - 3) {
			@Override
			public Double execute(Double left, Double right) {
				return left * right;
			}
		}).setDefault();

		operators.registerBinaryOperator(new BinaryOperator<Double>("/", MAX_PRIO - 3) {
			@Override
			public Double execute(Double left, Double right) {
				return left / right;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Double>("%", MAX_PRIO - 3) {
			@Override
			public Double execute(Double left, Double right) {
				return left % right;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Double>("^", MAX_PRIO - 2) {
			@Override
			public Double execute(Double left, Double right) {
				return Math.pow(left, right);
			}
		});

		final Calculator<Double, E> result = factory.create(NULL_VALUE, new DoubleParser(), operators);

		GenericFunctions.createStackManipulationFunctions(result);

		result.setGlobalSymbol("PI", Constant.create(Math.PI));
		result.setGlobalSymbol("E", Constant.create(Math.E));
		result.setGlobalSymbol("INF", Constant.create(Double.POSITIVE_INFINITY));
		result.setGlobalSymbol("MAX", Constant.create(Double.MIN_VALUE));

		result.setGlobalSymbol("abs", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.abs(value);
			}
		});

		result.setGlobalSymbol("sgn", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.signum(value);
			}
		});

		result.setGlobalSymbol("sqrt", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.sqrt(value);
			}
		});

		result.setGlobalSymbol("ceil", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.ceil(value);
			}
		});

		result.setGlobalSymbol("floor", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.floor(value);
			}
		});

		result.setGlobalSymbol("cos", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.cos(value);
			}
		});

		result.setGlobalSymbol("cosh", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.cosh(value);
			}
		});

		result.setGlobalSymbol("sin", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.sin(value);
			}
		});

		result.setGlobalSymbol("sinh", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.sinh(value);
			}
		});

		result.setGlobalSymbol("tan", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.tan(value);
			}
		});

		result.setGlobalSymbol("tanh", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.tanh(value);
			}
		});

		result.setGlobalSymbol("acos", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.acos(value);
			}
		});

		result.setGlobalSymbol("acosh", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.log(value + Math.sqrt(value * value - 1));
			}
		});

		result.setGlobalSymbol("asin", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.asin(value);
			}
		});

		result.setGlobalSymbol("asinh", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return value.isInfinite()? value : Math.log(value + Math.sqrt(value * value + 1));
			}
		});

		result.setGlobalSymbol("atan", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.atan(value);
			}
		});

		result.setGlobalSymbol("atanh", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.log((1 + value) / (1 - value)) / 2;
			}
		});

		result.setGlobalSymbol("atan2", new BinaryFunction<Double>() {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.atan2(left, right);
			}

		});

		result.setGlobalSymbol("log10", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.log10(value);
			}
		});

		result.setGlobalSymbol("ln", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.log(value);
			}
		});

		result.setGlobalSymbol("log", new BinaryFunction<Double>() {
			@Override
			protected Double execute(Double left, Double right) {
				return Math.log(left) / Math.log(right);
			}
		});

		result.setGlobalSymbol("exp", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.exp(value);
			}
		});

		result.setGlobalSymbol("min", new AccumulatorFunction<Double>(NULL_VALUE) {
			@Override
			protected Double accumulate(Double result, Double value) {
				return Math.min(result, value);
			}
		});

		result.setGlobalSymbol("max", new AccumulatorFunction<Double>(NULL_VALUE) {
			@Override
			protected Double accumulate(Double result, Double value) {
				return Math.max(result, value);
			}
		});

		result.setGlobalSymbol("sum", new AccumulatorFunction<Double>(NULL_VALUE) {
			@Override
			protected Double accumulate(Double result, Double value) {
				return result + value;
			}
		});

		result.setGlobalSymbol("avg", new AccumulatorFunction<Double>(NULL_VALUE) {
			@Override
			protected Double accumulate(Double result, Double value) {
				return result + value;
			}

			@Override
			protected Double process(Double result, int argCount) {
				return result / argCount;
			}
		});

		result.setGlobalSymbol("rad", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.toRadians(value);
			}
		});

		result.setGlobalSymbol("deg", new UnaryFunction<Double>() {
			@Override
			protected Double execute(Double value) {
				return Math.toDegrees(value);
			}
		});

		return result;
	}

	public static Calculator<Double, ExprType> createDefault() {
		return create(new BasicCalculatorFactory<Double, DoubleCalculator<ExprType>>() {
			@Override
			protected DoubleCalculator<ExprType> createCalculator(Double nullValue, Map<ExprType, ICompiler<Double>> compilers) {
				return new DoubleCalculator<ExprType>(compilers);
			}
		});
	}

}
