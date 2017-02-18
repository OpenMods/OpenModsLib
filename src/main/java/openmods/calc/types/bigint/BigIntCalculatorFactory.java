package openmods.calc.types.bigint;

import java.math.BigInteger;
import java.util.Random;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.Environment;
import openmods.calc.ExprType;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.IValuePrinter;
import openmods.calc.NullaryFunction;
import openmods.calc.OperatorDictionary;
import openmods.calc.SimpleCalculatorFactory;
import openmods.calc.TernaryFunction;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.BasicCompilerMapFactory;
import openmods.calc.parsing.CommonSimpleSymbolFactory;
import openmods.calc.parsing.IValueParser;

public class BigIntCalculatorFactory<M> extends SimpleCalculatorFactory<BigInteger, M> {

	public static final BigInteger NULL_VALUE = BigInteger.ZERO;

	@Override
	protected IValueParser<BigInteger> getValueParser() {
		return new BigIntParser();
	}

	@Override
	protected BigInteger getNullValue() {
		return NULL_VALUE;
	}

	@Override
	protected IValuePrinter<BigInteger> createValuePrinter() {
		return new BigIntPrinter();
	}

	@Override
	protected void configureEnvironment(Environment<BigInteger> env) {
		env.setGlobalSymbol("abs", new UnaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger value) {
				return value.abs();
			}
		});

		env.setGlobalSymbol("sgn", new UnaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger value) {
				return BigInteger.valueOf(value.signum());
			}
		});

		env.setGlobalSymbol("min", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.min(value);
			}
		});

		env.setGlobalSymbol("max", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.max(value);
			}
		});

		env.setGlobalSymbol("sum", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.add(value);
			}
		});

		env.setGlobalSymbol("avg", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.add(value);
			}

			@Override
			protected BigInteger process(BigInteger result, int argCount) {
				return result.divide(BigInteger.valueOf(argCount));
			}

		});

		env.setGlobalSymbol("gcd", new BinaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger left, BigInteger right) {
				return left.gcd(right);
			}
		});

		env.setGlobalSymbol("gcd", new BinaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger left, BigInteger right) {
				return left.gcd(right);
			}
		});

		env.setGlobalSymbol("modpow", new TernaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger first, BigInteger second, BigInteger third) {
				return first.modPow(second, third);
			}
		});

		env.setGlobalSymbol("get", new BinaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger first, BigInteger second) {
				return first.testBit(second.intValue())? BigInteger.ONE : BigInteger.ZERO;
			}
		});

		env.setGlobalSymbol("set", new BinaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger first, BigInteger second) {
				return first.setBit(second.intValue());
			}
		});

		env.setGlobalSymbol("clear", new BinaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger first, BigInteger second) {
				return first.clearBit(second.intValue());
			}
		});

		env.setGlobalSymbol("flip", new BinaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call(BigInteger first, BigInteger second) {
				return first.flipBit(second.intValue());
			}
		});

		final Random random = new Random();

		env.setGlobalSymbol("rand", new NullaryFunction.Direct<BigInteger>() {
			@Override
			protected BigInteger call() {
				return BigInteger.valueOf(random.nextLong());
			}
		});
	}

	private static final int PRIORITY_EXP = 6;
	private static final int PRIORITY_MULTIPLY = 5;
	private static final int PRIORITY_ADD = 4;
	private static final int PRIORITY_BITSHIFT = 3;
	private static final int PRIORITY_BITWISE = 2;
	private static final int PRIORITY_COLON = 1;

	@Override
	protected void configureOperators(OperatorDictionary<BigInteger> operators) {
		operators.registerUnaryOperator(new UnaryOperator.Direct<BigInteger>("~") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value.not();
			}
		});

		operators.registerUnaryOperator(new UnaryOperator.Direct<BigInteger>("neg") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("^", PRIORITY_BITWISE) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.xor(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("|", PRIORITY_BITWISE) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.or(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("&", PRIORITY_BITWISE) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.and(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("+", PRIORITY_ADD) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.add(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator.Direct<BigInteger>("+") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("-", PRIORITY_ADD) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.subtract(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator.Direct<BigInteger>("-") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("*", PRIORITY_MULTIPLY) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.multiply(right);
			}
		}).setDefault();

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("/", PRIORITY_MULTIPLY) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.divide(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("%", PRIORITY_MULTIPLY) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.mod(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("**", PRIORITY_EXP) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.pow(right.intValue());
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>("<<", PRIORITY_BITSHIFT) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftLeft(right.intValue());
			}
		});

		operators.registerBinaryOperator(new BinaryOperator.Direct<BigInteger>(">>", PRIORITY_BITSHIFT) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftRight(right.intValue());
			}
		});
	}

	public static Calculator<BigInteger, ExprType> createSimple() {
		return new BigIntCalculatorFactory<ExprType>().create(new BasicCompilerMapFactory<BigInteger>());
	}

	public static Calculator<BigInteger, ExprType> createDefault() {
		final CommonSimpleSymbolFactory<BigInteger> letFactory = new CommonSimpleSymbolFactory<BigInteger>(":", PRIORITY_COLON);

		return new BigIntCalculatorFactory<ExprType>() {
			@Override
			protected void configureOperators(OperatorDictionary<BigInteger> operators) {
				super.configureOperators(operators);
				operators.registerBinaryOperator(letFactory.getKeyValueSeparator());
			}
		}.create(letFactory.createCompilerFactory());
	}

}
