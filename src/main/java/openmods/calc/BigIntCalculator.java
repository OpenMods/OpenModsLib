package openmods.calc;

import java.math.BigInteger;

public class BigIntCalculator extends Calculator<BigInteger> {

	public BigIntCalculator() {
		super(new BigIntParser(), BigInteger.ZERO);
	}

	@Override
	protected void setupOperators(OperatorDictionary<BigInteger> operators) {
		operators.registerUnaryOperator("~", new UnaryOperator<BigInteger>(0) {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.not();
			}
		});

		operators.registerUnaryOperator("neg", new UnaryOperator<BigInteger>(0) {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator("^", new BinaryOperator<BigInteger>(2) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.xor(right);
			}
		});

		operators.registerBinaryOperator("|", new BinaryOperator<BigInteger>(2) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.or(right);
			}
		});

		operators.registerBinaryOperator("&", new BinaryOperator<BigInteger>(2) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.and(right);
			}
		});

		operators.registerMixedOperator("+", new BinaryOperator<BigInteger>(3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.add(right);
			}
		}, new UnaryOperator<BigInteger>(0) {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value;
			}
		});

		operators.registerMixedOperator("-", new BinaryOperator<BigInteger>(3) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.subtract(right);
			}
		}, new UnaryOperator<BigInteger>(0) {
			@Override
			protected BigInteger execute(BigInteger value) {
				return value.negate();
			}
		});

		operators.registerBinaryOperator("*", new BinaryOperator<BigInteger>(4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.multiply(right);
			}
		});

		operators.registerBinaryOperator("/", new BinaryOperator<BigInteger>(4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.divide(right);
			}
		});

		operators.registerBinaryOperator("%", new BinaryOperator<BigInteger>(4) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.mod(right);
			}
		});

		operators.registerBinaryOperator("**", new BinaryOperator<BigInteger>(5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.pow(right.intValue());
			}
		});

		operators.registerBinaryOperator("<<", new BinaryOperator<BigInteger>(5) {
			@Override
			protected BigInteger execute(BigInteger left, BigInteger right) {
				return left.shiftLeft(right.intValue());
			}
		});

		operators.registerBinaryOperator(">>", new BinaryOperator<BigInteger>(5) {
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

	}

}
