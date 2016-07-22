package openmods.calc.types.bigint;

import java.math.BigInteger;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.GenericFunctions;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.OperatorDictionary;
import openmods.calc.TernaryFunction;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.config.simpler.Configurable;

public class BigIntCalculator extends Calculator<BigInteger> {

	public static final BigInteger NULL_VALUE = BigInteger.ZERO;

	@Configurable
	public int base = 10;

	@Configurable
	public boolean uniformBaseNotation = false;

	private final BigIntPrinter printer = new BigIntPrinter();

	public BigIntCalculator(OperatorDictionary<BigInteger> operators, IExprNodeFactory<BigInteger> exprNodeFactory) {
		super(new BigIntParser(), NULL_VALUE, operators, exprNodeFactory);
	}

	@Override
	public String toString(BigInteger value) {
		if (base < Character.MIN_RADIX) return "invalid radix";
		return decorateBase(!uniformBaseNotation, base, (base <= Character.MAX_RADIX)? value.toString(base) : printer.toString(value, base));
	}

	private static final int MAX_PRIO = 6;

	public static BigIntCalculator create() {
		final OperatorDictionary<BigInteger> operators = new OperatorDictionary<BigInteger>();

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("~") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value.not();
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("neg") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("^", MAX_PRIO - 5) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.xor(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("|", MAX_PRIO - 5) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.or(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("&", MAX_PRIO - 5) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.and(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("+", MAX_PRIO - 4) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.add(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("+") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("-", MAX_PRIO - 4) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.subtract(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("-") {
			@Override
			public BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("*", MAX_PRIO - 3) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.multiply(right);
			}
		}).setDefault();

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("/", MAX_PRIO - 3) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.divide(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("%", MAX_PRIO - 3) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.mod(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("**", MAX_PRIO - 2) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.pow(right.intValue());
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("<<", MAX_PRIO - 1) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftLeft(right.intValue());
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>(">>", MAX_PRIO - 1) {
			@Override
			public BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftRight(right.intValue());
			}
		});

		final IExprNodeFactory<BigInteger> exprNodeFactory = new DefaultExprNodeFactory<BigInteger>();
		final BigIntCalculator result = new BigIntCalculator(operators, exprNodeFactory);

		GenericFunctions.createStackManipulationFunctions(result);

		result.setGlobalSymbol("abs", new UnaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.abs();
			}
		});

		result.setGlobalSymbol("sgn", new UnaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return BigInteger.valueOf(value.signum());
			}
		});

		result.setGlobalSymbol("min", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.min(value);
			}
		});

		result.setGlobalSymbol("max", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.max(value);
			}
		});

		result.setGlobalSymbol("sum", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.add(value);
			}
		});

		result.setGlobalSymbol("avg", new AccumulatorFunction<BigInteger>(NULL_VALUE) {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.add(value);
			}

			@Override
			protected BigInteger process(BigInteger result, int argCount) {
				return result.divide(BigInteger.valueOf(argCount));
			}

		});

		result.setGlobalSymbol("gcd", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.gcd(right);
			}
		});

		result.setGlobalSymbol("gcd", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.gcd(right);
			}
		});

		result.setGlobalSymbol("modpow", new TernaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second, BigInteger third) {
				return first.modPow(second, third);
			}
		});

		result.setGlobalSymbol("get", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.testBit(second.intValue())? BigInteger.ONE : BigInteger.ZERO;
			}
		});

		result.setGlobalSymbol("set", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.setBit(second.intValue());
			}
		});

		result.setGlobalSymbol("clear", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.clearBit(second.intValue());
			}
		});

		result.setGlobalSymbol("flip", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.flipBit(second.intValue());
			}
		});

		return result;
	}

}
