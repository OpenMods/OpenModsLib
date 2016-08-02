package openmods.calc.types.fraction;

import com.google.common.collect.Ordering;
import java.util.Map;
import openmods.calc.BasicCalculatorFactory;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.ExprType;
import openmods.calc.GenericFunctions;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.ICalculatorFactory;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.config.simpler.Configurable;
import org.apache.commons.lang3.math.Fraction;

public class FractionCalculator<M> extends Calculator<Fraction, M> {

	private static final Fraction NULL_VALUE = Fraction.ZERO;

	@Configurable
	public boolean properFractions;

	@Configurable
	public boolean expand;

	public FractionCalculator(Map<M, ICompiler<Fraction>> compilers) {
		super(NULL_VALUE, compilers);
	}

	private static Fraction createFraction(int value) {
		return Fraction.getFraction(value, 1);
	}

	@Override
	public String toString(Fraction value) {
		if (expand) return Double.toString(value.doubleValue());
		return properFractions? value.toProperString() : value.toString();
	}

	private static final int MAX_PRIO = 5;

	public static <E> Calculator<Fraction, E> create(ICalculatorFactory<Fraction, E, ? extends Calculator<Fraction, E>> factory) {
		final OperatorDictionary<Fraction> operators = new OperatorDictionary<Fraction>();

		operators.registerUnaryOperator(new UnaryOperator<Fraction>("neg") {
			@Override
			public Fraction execute(Fraction value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("+", MAX_PRIO - 4) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.add(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<Fraction>("+") {
			@Override
			public Fraction execute(Fraction value) {
				return value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("-", MAX_PRIO - 4) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.subtract(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<Fraction>("-") {
			@Override
			public Fraction execute(Fraction value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("*", MAX_PRIO - 3) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.multiplyBy(right);
			}
		}).setDefault();

		operators.registerBinaryOperator(new BinaryOperator<Fraction>("/", MAX_PRIO - 3) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.divideBy(right);
			}
		});

		final Calculator<Fraction, E> result = factory.create(NULL_VALUE, new FractionParser(), operators);

		GenericFunctions.createStackManipulationFunctions(result);

		result.setGlobalSymbol("abs", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return value.abs();
			}
		});

		result.setGlobalSymbol("sgn", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(Integer.signum(value.getNumerator()));
			}
		});

		result.setGlobalSymbol("numerator", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(value.getNumerator());
			}
		});

		result.setGlobalSymbol("denominator", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(value.getDenominator());
			}
		});

		result.setGlobalSymbol("frac", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return Fraction.getFraction(value.getProperNumerator(), value.getDenominator());
			}
		});

		result.setGlobalSymbol("int", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return createFraction(value.getProperWhole());
			}
		});

		result.setGlobalSymbol("sqrt", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return Fraction.getFraction(Math.sqrt(value.doubleValue()));
			}
		});

		result.setGlobalSymbol("log", new UnaryFunction<Fraction>() {
			@Override
			protected Fraction execute(Fraction value) {
				return Fraction.getFraction(Math.log(value.doubleValue()));
			}
		});

		result.setGlobalSymbol("min", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return Ordering.natural().min(result, value);
			}
		});

		result.setGlobalSymbol("max", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return Ordering.natural().max(result, value);
			}
		});

		result.setGlobalSymbol("sum", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return result.add(value);
			}
		});

		result.setGlobalSymbol("avg", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return result.add(value);
			}

			@Override
			protected Fraction process(Fraction result, int argCount) {
				return result.multiplyBy(Fraction.getFraction(1, argCount));
			}

		});

		return result;
	}

	public static Calculator<Fraction, ExprType> createDefault() {
		return create(new BasicCalculatorFactory<Fraction, FractionCalculator<ExprType>>() {
			@Override
			protected FractionCalculator<ExprType> createCalculator(Fraction nullValue, Map<ExprType, ICompiler<Fraction>> compilers) {
				return new FractionCalculator<ExprType>(compilers);
			}
		});
	}
}
