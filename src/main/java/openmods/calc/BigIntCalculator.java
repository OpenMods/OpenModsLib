package openmods.calc;

import java.math.BigInteger;

public class BigIntCalculator extends Calculator<BigInteger> {

	public BigIntCalculator() {
		super(new BigIntParser(), BigInteger.ZERO);
	}

	private static final int MAX_PRIO = 6;

	@Override
	protected void setupOperators(OperatorDictionary<BigInteger> operators) {
		operators.registerUnaryOperator("~", new UnaryOperator<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.not();
			}
		});

		operators.registerUnaryOperator("neg", new UnaryOperator<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator("^", new BinaryOperator<BigInteger>(MAX_PRIO - 5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.xor(right);
			}
		});

		operators.registerBinaryOperator("|", new BinaryOperator<BigInteger>(MAX_PRIO - 5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.or(right);
			}
		});

		operators.registerBinaryOperator("&", new BinaryOperator<BigInteger>(MAX_PRIO - 5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.and(right);
			}
		});

		operators.registerMixedOperator("+", new BinaryOperator<BigInteger>(MAX_PRIO - 4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.add(right);
			}
		}, new UnaryOperator<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value;
			}
		});

		operators.registerMixedOperator("-", new BinaryOperator<BigInteger>(MAX_PRIO - 4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.subtract(right);
			}
		}, new UnaryOperator<BigInteger>() {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator("*", new BinaryOperator<BigInteger>(MAX_PRIO - 3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.multiply(right);
			}
		});

		operators.registerBinaryOperator("/", new BinaryOperator<BigInteger>(MAX_PRIO - 3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.divide(right);
			}
		});

		operators.registerBinaryOperator("%", new BinaryOperator<BigInteger>(MAX_PRIO - 3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.mod(right);
			}
		});

		operators.registerBinaryOperator("**", new BinaryOperator<BigInteger>(MAX_PRIO - 2) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.pow(right.intValue());
			}
		});

		operators.registerBinaryOperator("<<", new BinaryOperator<BigInteger>(MAX_PRIO - 1) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftLeft(right.intValue());
			}
		});

		operators.registerBinaryOperator(">>", new BinaryOperator<BigInteger>(MAX_PRIO - 1) {
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

}
