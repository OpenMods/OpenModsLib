package openmods.calc.types.fraction;

import com.google.common.collect.Ordering;
import java.util.Random;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.Environment;
import openmods.calc.ExprType;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.IValuePrinter;
import openmods.calc.NullaryFunction;
import openmods.calc.OperatorDictionary;
import openmods.calc.SimpleCalculatorFactory;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.BasicCompilerMapFactory;
import openmods.calc.parsing.CommonSimpleSymbolFactory;
import openmods.calc.parsing.IValueParser;
import org.apache.commons.lang3.math.Fraction;

public class FractionCalculatorFactory<M> extends SimpleCalculatorFactory<Fraction, M> {
	public static final Fraction NULL_VALUE = Fraction.ZERO;

	private static Fraction int2frac(int value) {
		return Fraction.getFraction(value, 1);
	}

	@Override
	protected Fraction getNullValue() {
		return NULL_VALUE;
	}

	@Override
	protected IValueParser<Fraction> getValueParser() {
		return new FractionParser();
	}

	@Override
	protected IValuePrinter<Fraction> createValuePrinter() {
		return new FractionPrinter();
	}

	@Override
	protected void configureEnvironment(Environment<Fraction> env) {
		env.setGlobalSymbol("abs", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return value.abs();
			}
		});

		env.setGlobalSymbol("sgn", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return int2frac(Integer.signum(value.getNumerator()));
			}
		});

		env.setGlobalSymbol("numerator", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return int2frac(value.getNumerator());
			}
		});

		env.setGlobalSymbol("denominator", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return int2frac(value.getDenominator());
			}
		});

		env.setGlobalSymbol("frac", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return Fraction.getFraction(value.getProperNumerator(), value.getDenominator());
			}
		});

		env.setGlobalSymbol("int", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return int2frac(value.getProperWhole());
			}
		});

		env.setGlobalSymbol("sqrt", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return Fraction.getFraction(Math.sqrt(value.doubleValue()));
			}
		});

		env.setGlobalSymbol("log", new UnaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call(Fraction value) {
				return Fraction.getFraction(Math.log(value.doubleValue()));
			}
		});

		env.setGlobalSymbol("min", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return Ordering.natural().min(result, value);
			}
		});

		env.setGlobalSymbol("max", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return Ordering.natural().max(result, value);
			}
		});

		env.setGlobalSymbol("sum", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return result.add(value);
			}
		});

		env.setGlobalSymbol("avg", new AccumulatorFunction<Fraction>(NULL_VALUE) {
			@Override
			protected Fraction accumulate(Fraction result, Fraction value) {
				return result.add(value);
			}

			@Override
			protected Fraction process(Fraction result, int argCount) {
				return result.multiplyBy(Fraction.getFraction(1, argCount));
			}

		});

		final Random random = new Random();

		env.setGlobalSymbol("rand", new NullaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call() {
				return Fraction.getFraction(random.nextDouble());
			}
		});

		env.setGlobalSymbol("gauss", new NullaryFunction.Direct<Fraction>() {
			@Override
			protected Fraction call() {
				return Fraction.getFraction(random.nextGaussian());
			}
		});
	}

	private static final int PRIORITY_MULTIPLY = 2;
	private static final int PRIORITY_ADD = 1;
	private static final int PRIORITY_COLON = 0;

	@Override
	protected void configureOperators(OperatorDictionary<Fraction> operators) {
		operators.registerUnaryOperator(new UnaryOperator.Direct<Fraction>("neg") {
			@Override
			public Fraction execute(Fraction value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<Fraction>("+", PRIORITY_ADD) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.add(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator.Direct<Fraction>("+") {
			@Override
			public Fraction execute(Fraction value) {
				return value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<Fraction>("-", PRIORITY_ADD) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.subtract(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator.Direct<Fraction>("-") {
			@Override
			public Fraction execute(Fraction value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<Fraction>("*", PRIORITY_MULTIPLY) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.multiplyBy(right);
			}
		}).setDefault();

		operators.registerBinaryOperator(new BinaryOperator.Direct<Fraction>("/", PRIORITY_MULTIPLY) {
			@Override
			public Fraction execute(Fraction left, Fraction right) {
				return left.divideBy(right);
			}
		});
	}

	public static Calculator<Fraction, ExprType> createSimple() {
		return new FractionCalculatorFactory<ExprType>().create(new BasicCompilerMapFactory<Fraction>());
	}

	public static Calculator<Fraction, ExprType> createDefault() {
		final CommonSimpleSymbolFactory<Fraction> letFactory = new CommonSimpleSymbolFactory<Fraction>(":", PRIORITY_COLON);

		return new FractionCalculatorFactory<ExprType>() {
			@Override
			protected void configureOperators(OperatorDictionary<Fraction> operators) {
				super.configureOperators(operators);
				operators.registerBinaryOperator(letFactory.getKeyValueSeparator());
			}
		}.create(letFactory.createCompilerFactory());
	}
}
