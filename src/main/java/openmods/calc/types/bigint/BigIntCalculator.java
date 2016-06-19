package openmods.calc.types.bigint;

import java.math.BigInteger;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.IValueParser;
import openmods.calc.OperatorDictionary;
import openmods.calc.TernaryFunction;
import openmods.calc.TopFrame;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.ICompiler;
import openmods.calc.parsing.InfixCompiler;
import openmods.config.simpler.Configurable;

public class BigIntCalculator extends Calculator<BigInteger> {

	@Configurable
	public int base = 10;

	@Configurable
	public boolean uniformBaseNotation = false;

	private final BigIntPrinter printer = new BigIntPrinter();

	public BigIntCalculator() {
		super(new BigIntParser(), BigInteger.ZERO);
	}

	private static final int MAX_PRIO = 6;

	@Override
	protected ICompiler<BigInteger> createInfixCompiler(IValueParser<BigInteger> valueParser, OperatorDictionary<BigInteger> operators) {
		return new InfixCompiler<BigInteger>(valueParser, operators);
	}

	@Override
	protected void setupOperators(OperatorDictionary<BigInteger> operators) {
		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("~") {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.not();
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("neg") {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("^", MAX_PRIO - 5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.xor(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("|", MAX_PRIO - 5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.or(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("&", MAX_PRIO - 5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.and(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("+", MAX_PRIO - 4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.add(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("+") {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("-", MAX_PRIO - 4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.subtract(right);
			}
		});

		operators.registerUnaryOperator(new UnaryOperator<BigInteger>("-") {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("*", MAX_PRIO - 3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.multiply(right);
			}
		}).setDefault();

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("/", MAX_PRIO - 3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.divide(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("%", MAX_PRIO - 3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.mod(right);
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("**", MAX_PRIO - 2) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.pow(right.intValue());
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>("<<", MAX_PRIO - 1) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftLeft(right.intValue());
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<BigInteger>(">>", MAX_PRIO - 1) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftRight(right.intValue());
			}
		});
	}

	@Override
	protected void setupGlobals(TopFrame<BigInteger> globals) {

		globals.setSymbol("abs", new UnaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.abs();
			}
		});

		globals.setSymbol("sgn", new UnaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return BigInteger.valueOf(value.signum());
			}
		});

		globals.setSymbol("min", new AccumulatorFunction() {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.min(value);
			}
		});

		globals.setSymbol("max", new AccumulatorFunction() {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.max(value);
			}
		});

		globals.setSymbol("sum", new AccumulatorFunction() {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.add(value);
			}
		});

		globals.setSymbol("avg", new AccumulatorFunction() {
			@Override
			protected BigInteger accumulate(BigInteger result, BigInteger value) {
				return result.add(value);
			}

			@Override
			protected BigInteger process(BigInteger result, int argCount) {
				return result.divide(BigInteger.valueOf(argCount));
			}

		});

		globals.setSymbol("gcd", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.gcd(right);
			}
		});

		globals.setSymbol("gcd", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.gcd(right);
			}
		});

		globals.setSymbol("modpow", new TernaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second, BigInteger third) {
				return first.modPow(second, third);
			}
		});

		globals.setSymbol("get", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.testBit(second.intValue())? BigInteger.ONE : BigInteger.ZERO;
			}
		});

		globals.setSymbol("set", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.setBit(second.intValue());
			}
		});

		globals.setSymbol("clear", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.clearBit(second.intValue());
			}
		});

		globals.setSymbol("flip", new BinaryFunction<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger first, BigInteger second) {
				return first.flipBit(second.intValue());
			}
		});

	}

	@Override
	public String toString(BigInteger value) {
		if (base < Character.MIN_RADIX) return "invalid radix";
		return decorateBase(!uniformBaseNotation, base, (base <= Character.MAX_RADIX)? value.toString(base) : printer.toString(value, base));
	}

}
