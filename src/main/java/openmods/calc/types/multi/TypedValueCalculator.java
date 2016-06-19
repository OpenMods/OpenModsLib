package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import java.math.BigInteger;
import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.Constant;
import openmods.calc.OperatorDictionary;
import openmods.calc.TopFrame;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.StringEscaper;
import openmods.calc.types.bigint.BigIntPrinter;
import openmods.calc.types.fp.DoublePrinter;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.calc.types.multi.TypeDomain.ITruthEvaluator;
import openmods.config.simpler.Configurable;
import org.apache.commons.lang3.StringUtils;

public class TypedValueCalculator extends Calculator<TypedValue> {

	public static class UnitType implements Comparable<UnitType> {
		public static final UnitType INSTANCE = new UnitType();

		private UnitType() {}

		@Override
		public String toString() {
			return "<null>";
		}

		@Override
		public int compareTo(UnitType o) {
			return 0; // always equal
		}
	}

	private static final TCharSet UNESCAPED_CHARS = new TCharHashSet(new char[] { '\'' });

	@Configurable
	public int base = 10;

	@Configurable
	public boolean uniformBaseNotation = false;

	@Configurable
	public boolean allowStandardPrinter = false;

	@Configurable
	public boolean escapeStrings = true;

	@Configurable
	public boolean numericBool = false;

	@Configurable
	public boolean printTypes = true;

	private final DoublePrinter doublePrinter = new DoublePrinter(8);

	private final BigIntPrinter bigIntPrinter = new BigIntPrinter();

	public static TypedValueCalculator create() {
		TypeDomain domain = new TypeDomain();
		setupTypeDomain(domain);
		return new TypedValueCalculator(domain);
	}

	private TypedValueCalculator(TypeDomain domain) {
		super(new TypedValueParser(domain), domain.create(UnitType.class, UnitType.INSTANCE));
	}

	private static final int MAX_PRIO = 6;

	private static void setupTypeDomain(TypeDomain domain) {
		domain.registerType(UnitType.class);
		domain.registerType(BigInteger.class);
		domain.registerType(Double.class);
		domain.registerType(Boolean.class);
		domain.registerType(String.class);

		domain.registerConverter(new IConverter<BigInteger, Double>() {
			@Override
			public Double convert(BigInteger value) {
				return value.doubleValue();
			}
		});
		domain.registerConverter(new IConverter<Boolean, BigInteger>() {
			@Override
			public BigInteger convert(Boolean value) {
				return value? BigInteger.ONE : BigInteger.ZERO;
			}
		});
		domain.registerConverter(new IConverter<Boolean, Double>() {
			@Override
			public Double convert(Boolean value) {
				return value? 1.0 : 0.0;
			}
		});

		domain.registerSymmetricCoercionRule(BigInteger.class, Double.class, Coercion.TO_RIGHT);
		domain.registerSymmetricCoercionRule(Boolean.class, Double.class, Coercion.TO_RIGHT);
		domain.registerSymmetricCoercionRule(Boolean.class, BigInteger.class, Coercion.TO_RIGHT);

		domain.registerTruthEvaluator(new ITruthEvaluator<Boolean>() {
			@Override
			public boolean isTruthy(Boolean value) {
				return value.booleanValue();
			}
		});

		domain.registerTruthEvaluator(new ITruthEvaluator<BigInteger>() {
			@Override
			public boolean isTruthy(BigInteger value) {
				return !value.equals(BigInteger.ZERO);
			}
		});

		domain.registerTruthEvaluator(new ITruthEvaluator<Double>() {
			@Override
			public boolean isTruthy(Double value) {
				return value != 0;
			}
		});

		domain.registerTruthEvaluator(new ITruthEvaluator<UnitType>() {
			@Override
			public boolean isTruthy(UnitType value) {
				return false;
			}
		});

		domain.registerTruthEvaluator(new ITruthEvaluator<String>() {
			@Override
			public boolean isTruthy(String value) {
				return !value.isEmpty();
			}
		});
	}

	private interface CompareTranslator {
		public boolean translate(int value);
	}

	private static <T extends Comparable<T>> TypedBinaryOperator.ISimpleCoercedOperation<T, Boolean> createCompareOperation(final CompareTranslator compareTranslator) {
		return new TypedBinaryOperator.ISimpleCoercedOperation<T, Boolean>() {
			@Override
			public Boolean apply(T left, T right) {
				return compareTranslator.translate(left.compareTo(right));
			}
		};
	}

	private static TypedBinaryOperator createCompareOperator(TypeDomain domain, String id, int priority, final CompareTranslator compareTranslator) {
		return new TypedBinaryOperator.Builder(id, priority)
				.registerOperation(BigInteger.class, Boolean.class, TypedValueCalculator.<BigInteger> createCompareOperation(compareTranslator))
				.registerOperation(Double.class, Boolean.class, TypedValueCalculator.<Double> createCompareOperation(compareTranslator))
				.registerOperation(String.class, Boolean.class, TypedValueCalculator.<String> createCompareOperation(compareTranslator))
				.registerOperation(Boolean.class, Boolean.class, TypedValueCalculator.<Boolean> createCompareOperation(compareTranslator))
				.registerOperation(UnitType.class, Boolean.class, TypedValueCalculator.<UnitType> createCompareOperation(compareTranslator))
				.build(domain);
	}

	private interface VariantCompareTranslator {
		public TypedValue translate(TypeDomain domain, int value);
	}

	private static <T extends Comparable<T>> TypedBinaryOperator.ICoercedOperation<T> createCompareOperation(final VariantCompareTranslator compareTranslator) {
		return new TypedBinaryOperator.ICoercedOperation<T>() {
			@Override
			public TypedValue apply(TypeDomain domain, T left, T right) {
				return compareTranslator.translate(domain, left.compareTo(right));
			}
		};
	}

	private static TypedUnaryOperator createUnaryNegation(String id, TypeDomain domain) {
		return new TypedUnaryOperator.Builder(id)
				.registerOperation(new TypedUnaryOperator.ISimpleOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger value) {
						return value.negate();
					}
				})
				.registerOperation(new TypedUnaryOperator.ISimpleOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean value) {
						return value? BigInteger.valueOf(-1) : BigInteger.ZERO;
					}
				})
				.registerOperation(new TypedUnaryOperator.ISimpleOperation<Double, Double>() {
					@Override
					public Double apply(Double value) {
						return -value;
					}

				})
				.build(domain);
	}

	@Override
	protected void setupOperators(OperatorDictionary<TypedValue> operators) {
		final TypeDomain domain = nullValue().domain;

		// arithmetic
		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("+", MAX_PRIO - 2)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.add(right);
					}
				}).registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left + right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<String, String>() {
					@Override
					public String apply(String left, String right) {
						return left + right;
					}
				}).registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {

					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) + (right? 1 : 0));
					}
				})
				.build(domain));

		operators.registerUnaryOperator(new UnaryOperator<TypedValue>("+") {
			@Override
			protected TypedValue execute(TypedValue value) {
				Preconditions.checkState(Number.class.isAssignableFrom(value.type) || Boolean.class.isAssignableFrom(value.type),
						"Not a number: %s", value);
				return value;
			}
		});

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("-", MAX_PRIO - 2)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.subtract(right);
					}
				}).registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left - right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) - (right? 1 : 0));
					}
				})
				.build(domain));

		operators.registerUnaryOperator(createUnaryNegation("-", domain));

		operators.registerUnaryOperator(createUnaryNegation("neg", domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("*", MAX_PRIO - 1)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.multiply(right);
					}
				}).registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left * right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) * (right? 1 : 0));
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleVariantOperation<String, BigInteger, String>() {

					@Override
					public String apply(String left, BigInteger right) {
						return StringUtils.repeat(left, right.intValue());
					}
				})
				.build(domain)).setDefault();

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("/", MAX_PRIO - 1)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left / right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, Double>() {
					@Override
					public Double apply(BigInteger left, BigInteger right) {
						return left.doubleValue() / right.doubleValue();
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, Double>() {
					@Override
					public Double apply(Boolean left, Boolean right) {
						return (left? 1.0 : 0.0) / (right? 1.0 : 0.0);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("%", MAX_PRIO - 1)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) % (right? 1 : 0));
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.mod(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left % right;
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("//", MAX_PRIO - 1)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) / (right? 1 : 0));
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.divide(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return Math.floor(left / right);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("**", MAX_PRIO)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 0 : 1) * (right? 0 : 1));
					}
				})
				.registerOperation(new TypedBinaryOperator.ICoercedOperation<BigInteger>() {
					@Override
					public TypedValue apply(TypeDomain domain, BigInteger left, BigInteger right) {
						final int exp = right.intValue();
						return exp >= 0? domain.create(BigInteger.class, left.pow(exp)) : domain.create(Double.class, Math.pow(left.doubleValue(), exp));
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return Math.pow(left, right);
					}
				})
				.build(domain));

		// logic

		operators.registerUnaryOperator(new UnaryOperator<TypedValue>("!") {
			@Override
			protected TypedValue execute(TypedValue value) {
				final Optional<Boolean> isTruthy = value.isTruthy();
				Preconditions.checkState(isTruthy.isPresent(), "Can't determine truth value for %s", value);
				return value.domain.create(Boolean.class, !isTruthy.get());
			}
		});

		operators.registerBinaryOperator(new TypedBinaryBooleanOperator("&&", MAX_PRIO - 6) {
			@Override
			protected TypedValue execute(boolean isTruthy, TypedValue left, TypedValue right) {
				return isTruthy? right : left;
			}
		});

		operators.registerBinaryOperator(new TypedBinaryBooleanOperator("||", MAX_PRIO - 6) {
			@Override
			protected TypedValue execute(boolean isTruthy, TypedValue left, TypedValue right) {
				return isTruthy? left : right;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<TypedValue>("^^", MAX_PRIO - 6) {
			@Override
			protected TypedValue execute(TypedValue left, TypedValue right) {
				Preconditions.checkArgument(left.domain == right.domain, "Values from different domains: %s, %s", left, right);
				final Optional<Boolean> isLeftTruthy = left.isTruthy();
				Preconditions.checkState(isLeftTruthy.isPresent(), "Can't determine truth value for %s", left);

				final Optional<Boolean> isRightTruthy = right.isTruthy();
				Preconditions.checkState(isRightTruthy.isPresent(), "Can't determine truth value for %s", right);
				return left.domain.create(Boolean.class, isLeftTruthy.get() ^ isRightTruthy.get());
			}
		});

		// bitwise

		operators.registerUnaryOperator(new TypedUnaryOperator.Builder("~")
				.registerOperation(new TypedUnaryOperator.ISimpleOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean value) {
						return value? BigInteger.ZERO : BigInteger.ONE;
					}
				})
				.registerOperation(new TypedUnaryOperator.ISimpleOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger value) {
						return value.not();
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("&", MAX_PRIO - 4)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return (left & right)? BigInteger.ONE : BigInteger.ZERO;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.and(right);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("|", MAX_PRIO - 4)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return (left | right)? BigInteger.ONE : BigInteger.ZERO;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.or(right);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("^", MAX_PRIO - 4)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return (left ^ right)? BigInteger.ONE : BigInteger.ZERO;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.xor(right);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("<<", MAX_PRIO - 3)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) << (right? 1 : 0));
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.shiftLeft(right.intValue());
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder(">>", MAX_PRIO - 3)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {
					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) >> (right? 1 : 0));
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.shiftRight(right.intValue());
					}
				})
				.build(domain));

		// comparision

		operators.registerBinaryOperator(createCompareOperator(domain, "<", MAX_PRIO - 5, new CompareTranslator() {
			@Override
			public boolean translate(int value) {
				return value < 0;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, ">", MAX_PRIO - 5, new CompareTranslator() {
			@Override
			public boolean translate(int value) {
				return value > 0;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, "==", MAX_PRIO - 5, new CompareTranslator() {
			@Override
			public boolean translate(int value) {
				return value == 0;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, "!=", MAX_PRIO - 5, new CompareTranslator() {
			@Override
			public boolean translate(int value) {
				return value != 0;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, "<=", MAX_PRIO - 5, new CompareTranslator() {
			@Override
			public boolean translate(int value) {
				return value <= 0;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, ">=", MAX_PRIO - 5, new CompareTranslator() {
			@Override
			public boolean translate(int value) {
				return value >= 0;
			}
		}));

		{
			final VariantCompareTranslator compareResultTranslator = new VariantCompareTranslator() {
				@Override
				public TypedValue translate(TypeDomain domain, int value) {
					return domain.create(BigInteger.class, BigInteger.valueOf(value));
				}
			};

			operators.registerBinaryOperator(new TypedBinaryOperator.Builder("<=>", MAX_PRIO - 5)
					.registerOperation(BigInteger.class, TypedValueCalculator.<BigInteger> createCompareOperation(compareResultTranslator))
					.registerOperation(Double.class, TypedValueCalculator.<Double> createCompareOperation(compareResultTranslator))
					.registerOperation(String.class, TypedValueCalculator.<String> createCompareOperation(compareResultTranslator))
					.registerOperation(Boolean.class, TypedValueCalculator.<Boolean> createCompareOperation(compareResultTranslator))
					.registerOperation(UnitType.class, TypedValueCalculator.<UnitType> createCompareOperation(compareResultTranslator))
					.build(domain));
		}
	}

	@Override
	protected void setupGlobals(TopFrame<TypedValue> globals) {
		final TypedValue nullValue = nullValue();
		final TypeDomain domain = nullValue.domain;

		globals.setSymbol("null", Constant.create(nullValue));
		globals.setSymbol("true", Constant.create(domain.create(Boolean.class, Boolean.TRUE)));
		globals.setSymbol("false", Constant.create(domain.create(Boolean.class, Boolean.FALSE)));
	}

	@Override
	public String toString(TypedValue value) {
		final String contents;
		if (value.type == Double.class) contents = printDouble(value.unwrap(Double.class));
		else if (value.type == BigInteger.class) contents = printBigInteger(value.unwrap(BigInteger.class));
		else if (value.type == String.class) contents = printString(value.unwrap(String.class));
		else if (value.type == Boolean.class) contents = printBoolean(value.unwrap(Boolean.class));
		else contents = value.value.toString();

		return printTypes? "(" + value.type + ")" + contents : contents;
	}

	private String printBoolean(boolean value) {
		return numericBool? (value? "1" : "0") : (value? "True" : "False");
	}

	private String printString(String value) {
		return escapeStrings? StringEscaper.escapeString(value, '"', UNESCAPED_CHARS) : value;
	}

	private String printBigInteger(BigInteger value) {
		if (base < Character.MIN_RADIX) return "invalid radix";
		return decorateBase(!uniformBaseNotation, base, (base <= Character.MAX_RADIX)? value.toString(base) : bigIntPrinter.toString(value, base));
	}

	private String printDouble(Double value) {
		if (base == 10 && !allowStandardPrinter && !uniformBaseNotation) {
			return value.toString();
		} else {
			if (value.isNaN()) return "NaN";
			if (value.isInfinite()) return value > 0? "+Inf" : "-Inf";
			final String result = doublePrinter.toString(value, base);
			return decorateBase(!uniformBaseNotation, base, result);
		}
	}
}
