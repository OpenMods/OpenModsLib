package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.BinaryOperator.Associativity;
import openmods.calc.Calculator;
import openmods.calc.Compilers;
import openmods.calc.Environment;
import openmods.calc.ExecutionErrorException;
import openmods.calc.ExprType;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.GenericFunctions;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.ICallable;
import openmods.calc.ISymbol;
import openmods.calc.OperatorDictionary;
import openmods.calc.StackValidationException;
import openmods.calc.TopSymbolMap;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.BasicCompilerMapFactory;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.DefaultExecutableListBuilder;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.DefaultPostfixCompiler;
import openmods.calc.parsing.DefaultPostfixCompiler.IStateProvider;
import openmods.calc.parsing.IExecutableListBuilder;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IPostfixCompilerState;
import openmods.calc.parsing.ITokenStreamCompiler;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.MappedCompilerState;
import openmods.calc.parsing.MappedExprNodeFactory;
import openmods.calc.parsing.MappedExprNodeFactory.IBinaryExprNodeFactory;
import openmods.calc.parsing.MappedExprNodeFactory.IBracketExprNodeFactory;
import openmods.calc.parsing.SquareBracketContainerNode;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.Tokenizer;
import openmods.calc.parsing.ValueNode;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.calc.types.multi.TypeDomain.ITruthEvaluator;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.OptionalArgs;
import openmods.calc.types.multi.TypedFunction.RawDispatchArg;
import openmods.calc.types.multi.TypedFunction.RawReturn;
import openmods.calc.types.multi.TypedFunction.Variant;
import openmods.math.Complex;
import openmods.utils.Stack;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class TypedValueCalculatorFactory {
	private static final Function<BigInteger, Integer> INT_UNWRAP = new Function<BigInteger, Integer>() {
		@Override
		public Integer apply(BigInteger input) {
			return input.intValue();
		}
	};

	private static final int PRIORITY_MAX = 12; // basically magic
	private static final int PRIORITY_DOT = 11; // .
	private static final int PRIORITY_EXP = 10; // **
	private static final int PRIORITY_MULTIPLY = 9; // * / % //
	private static final int PRIORITY_ADD = 8; // + -
	private static final int PRIORITY_BITSHIFT = 7; // << >>
	private static final int PRIORITY_BITWISE = 6; // & ^ |
	private static final int PRIORITY_COMPARE = 5; // < > <= >= <=>
	private static final int PRIORITY_SPACESHIP = 4; // <=>
	private static final int PRIORITY_EQUALS = 3; // == !=
	private static final int PRIORITY_LOGIC = 2; // && || ^^
	private static final int PRIORITY_LAMBDA = 1; // ->
	private static final int PRIORITY_CONS = 0; // :

	private static class MarkerBinaryOperator extends BinaryOperator<TypedValue> {
		private MarkerBinaryOperator(String id, int precendence) {
			super(id, precendence);
		}

		public MarkerBinaryOperator(String id, int precedence, Associativity associativity) {
			super(id, precedence, associativity);
		}

		@Override
		public TypedValue execute(TypedValue left, TypedValue right) {
			throw new UnsupportedOperationException(); // should be replaced in AST tree modification
		}
	}

	private interface CompareResultInterpreter {
		public boolean interpret(int value);
	}

	private static <T extends Comparable<T>> TypedBinaryOperator.ISimpleCoercedOperation<T, Boolean> createCompareOperation(final CompareResultInterpreter interpreter) {
		return new TypedBinaryOperator.ISimpleCoercedOperation<T, Boolean>() {
			@Override
			public Boolean apply(T left, T right) {
				return interpreter.interpret(left.compareTo(right));
			}
		};
	}

	private static TypedBinaryOperator createCompareOperator(TypeDomain domain, String id, int priority, final CompareResultInterpreter compareTranslator) {
		return new TypedBinaryOperator.Builder(id, priority)
				.registerOperation(BigInteger.class, Boolean.class, TypedValueCalculatorFactory.<BigInteger> createCompareOperation(compareTranslator))
				.registerOperation(Double.class, Boolean.class, TypedValueCalculatorFactory.<Double> createCompareOperation(compareTranslator))
				.registerOperation(String.class, Boolean.class, TypedValueCalculatorFactory.<String> createCompareOperation(compareTranslator))
				.registerOperation(Boolean.class, Boolean.class, TypedValueCalculatorFactory.<Boolean> createCompareOperation(compareTranslator))
				.build(domain);
	}

	private interface EqualsResultInterpreter {
		public boolean interpret(boolean isEqual);
	}

	private static TypedBinaryOperator createEqualsOperator(TypeDomain domain, String id, int priority, final EqualsResultInterpreter equalsTranslator) {
		return new TypedBinaryOperator.Builder(id, priority)
				.setDefaultOperation(new TypedBinaryOperator.IDefaultOperation() {
					@Override
					public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right) {
						return Optional.of(domain.create(Boolean.class, equalsTranslator.interpret(left.equals(right))));
					}
				})
				.build(domain);
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
				.registerOperation(new TypedUnaryOperator.ISimpleOperation<Complex, Complex>() {
					@Override
					public Complex apply(Complex value) {
						return value.negate();
					}

				})
				.build(domain);
	}

	private static final Set<Class<?>> NUMBER_TYPES = ImmutableSet.<Class<?>> of(Double.class, Boolean.class, BigInteger.class, Complex.class);

	private static boolean isNumericValueNode(IExprNode<TypedValue> node) {
		if (node instanceof ValueNode) {
			final ValueNode<TypedValue> valueNode = (ValueNode<TypedValue>)node;
			return NUMBER_TYPES.contains(valueNode.value.type);
		}

		return false;
	}

	private static class PredicateIsType extends UnaryFunction<TypedValue> {
		private final Class<?> cls;

		public PredicateIsType(Class<?> cls) {
			this.cls = cls;
		}

		@Override
		protected TypedValue call(TypedValue value) {
			return value.domain.create(Boolean.class, value.is(cls));
		}
	}

	public static Calculator<TypedValue, ExprType> create() {
		final TypeDomain domain = new TypeDomain();

		domain.registerType(UnitType.class, "<null>");
		domain.registerType(BigInteger.class, "int");
		domain.registerType(Double.class, "float");
		domain.registerType(Boolean.class, "bool");
		domain.registerType(String.class, "str");
		domain.registerType(Complex.class, "complex");
		domain.registerType(IComposite.class, "object");
		domain.registerType(Cons.class, "pair");
		domain.registerType(Symbol.class, "symbol");
		domain.registerType(Code.class, "code");
		domain.registerType(ICallable.class, "callable");

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
		domain.registerConverter(new IConverter<Boolean, Complex>() {
			@Override
			public Complex convert(Boolean value) {
				return value? Complex.ONE : Complex.ZERO;
			}
		});

		domain.registerConverter(new IConverter<BigInteger, Double>() {
			@Override
			public Double convert(BigInteger value) {
				return value.doubleValue();
			}
		});
		domain.registerConverter(new IConverter<BigInteger, Complex>() {
			@Override
			public Complex convert(BigInteger value) {
				return Complex.real(value.doubleValue());
			}
		});

		domain.registerConverter(new IConverter<Double, Complex>() {
			@Override
			public Complex convert(Double value) {
				return Complex.real(value.doubleValue());
			}
		});

		domain.registerSymmetricCoercionRule(Boolean.class, BigInteger.class, Coercion.TO_RIGHT);
		domain.registerSymmetricCoercionRule(Boolean.class, Double.class, Coercion.TO_RIGHT);
		domain.registerSymmetricCoercionRule(Boolean.class, Complex.class, Coercion.TO_RIGHT);

		domain.registerSymmetricCoercionRule(BigInteger.class, Double.class, Coercion.TO_RIGHT);
		domain.registerSymmetricCoercionRule(BigInteger.class, Complex.class, Coercion.TO_RIGHT);

		domain.registerSymmetricCoercionRule(Double.class, Complex.class, Coercion.TO_RIGHT);

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

		domain.registerTruthEvaluator(new ITruthEvaluator<Complex>() {
			@Override
			public boolean isTruthy(Complex value) {
				return !value.equals(Complex.ZERO);
			}
		});

		domain.registerTruthEvaluator(new ITruthEvaluator<String>() {
			@Override
			public boolean isTruthy(String value) {
				return !value.isEmpty();
			}
		});

		domain.registerAlwaysFalse(UnitType.class);
		domain.registerAlwaysTrue(IComposite.class);
		domain.registerAlwaysTrue(Cons.class);
		domain.registerAlwaysTrue(Code.class);
		domain.registerAlwaysTrue(ICallable.class);

		final TypedValue nullValue = domain.create(UnitType.class, UnitType.INSTANCE);

		final OperatorDictionary<TypedValue> operators = new OperatorDictionary<TypedValue>();

		// arithmetic
		final BinaryOperator<TypedValue> addOperator = operators.registerBinaryOperator(new TypedBinaryOperator.Builder("+", PRIORITY_ADD)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.add(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Complex, Complex>() {
					@Override
					public Complex apply(Complex left, Complex right) {
						return left.add(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
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
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, BigInteger>() {

					@Override
					public BigInteger apply(Boolean left, Boolean right) {
						return BigInteger.valueOf((left? 1 : 0) + (right? 1 : 0));
					}
				})
				.build(domain)).unwrap();

		operators.registerUnaryOperator(new UnaryOperator<TypedValue>("+") {
			@Override
			public TypedValue execute(TypedValue value) {
				Preconditions.checkState(NUMBER_TYPES.contains(value.type), "Not a number: %s", value);
				return value;
			}
		});

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("-", PRIORITY_ADD)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.subtract(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left - right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Complex, Complex>() {
					@Override
					public Complex apply(Complex left, Complex right) {
						return left.subtract(right);
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

		final BinaryOperator<TypedValue> multiplyOperator = operators.registerBinaryOperator(new TypedBinaryOperator.Builder("*", PRIORITY_MULTIPLY)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.multiply(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Double, Double>() {
					@Override
					public Double apply(Double left, Double right) {
						return left * right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Complex, Complex>() {
					@Override
					public Complex apply(Complex left, Complex right) {
						return left.multiply(right);
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
				.build(domain)).unwrap();

		final BinaryOperator<TypedValue> divideOperator = operators.registerBinaryOperator(new TypedBinaryOperator.Builder("/", PRIORITY_MULTIPLY)
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
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Complex, Complex>() {
					@Override
					public Complex apply(Complex left, Complex right) {
						return left.divide(right);
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, Double>() {
					@Override
					public Double apply(Boolean left, Boolean right) {
						return (left? 1.0 : 0.0) / (right? 1.0 : 0.0);
					}
				})
				.build(domain)).unwrap();

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("%", PRIORITY_MULTIPLY)
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("//", PRIORITY_MULTIPLY)
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("**", PRIORITY_EXP)
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
			public TypedValue execute(TypedValue value) {
				final Optional<Boolean> isTruthy = value.isTruthy();
				Preconditions.checkState(isTruthy.isPresent(), "Can't determine truth value for %s", value);
				return value.domain.create(Boolean.class, !isTruthy.get());
			}
		});

		operators.registerBinaryOperator(new TypedBinaryBooleanOperator("&&", PRIORITY_LOGIC) {
			@Override
			protected TypedValue execute(boolean isTruthy, TypedValue left, TypedValue right) {
				return isTruthy? right : left;
			}
		});

		operators.registerBinaryOperator(new TypedBinaryBooleanOperator("||", PRIORITY_LOGIC) {
			@Override
			protected TypedValue execute(boolean isTruthy, TypedValue left, TypedValue right) {
				return isTruthy? left : right;
			}
		});

		operators.registerBinaryOperator(new BinaryOperator<TypedValue>("^^", PRIORITY_LOGIC) {
			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("&", PRIORITY_BITWISE)
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("|", PRIORITY_BITWISE)
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("^", PRIORITY_BITWISE)
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("<<", PRIORITY_BITSHIFT)
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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder(">>", PRIORITY_BITSHIFT)
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

		final BinaryOperator<TypedValue> ltOperator = operators.registerBinaryOperator(createCompareOperator(domain, "<", PRIORITY_COMPARE, new CompareResultInterpreter() {
			@Override
			public boolean interpret(int value) {
				return value < 0;
			}
		})).unwrap();

		final BinaryOperator<TypedValue> gtOperator = operators.registerBinaryOperator(createCompareOperator(domain, ">", PRIORITY_COMPARE, new CompareResultInterpreter() {
			@Override
			public boolean interpret(int value) {
				return value > 0;
			}
		})).unwrap();

		operators.registerBinaryOperator(createEqualsOperator(domain, "==", PRIORITY_EQUALS, new EqualsResultInterpreter() {
			@Override
			public boolean interpret(boolean isEqual) {
				return isEqual;
			}
		}));

		operators.registerBinaryOperator(createEqualsOperator(domain, "!=", PRIORITY_EQUALS, new EqualsResultInterpreter() {
			@Override
			public boolean interpret(boolean isEqual) {
				return !isEqual;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, "<=", PRIORITY_COMPARE, new CompareResultInterpreter() {
			@Override
			public boolean interpret(int value) {
				return value <= 0;
			}
		}));

		operators.registerBinaryOperator(createCompareOperator(domain, ">=", PRIORITY_COMPARE, new CompareResultInterpreter() {
			@Override
			public boolean interpret(int value) {
				return value >= 0;
			}
		}));

		// magic

		final BinaryOperator<TypedValue> dotOperator = operators.registerBinaryOperator(new TypedBinaryOperator.Builder(".", PRIORITY_DOT)
				.registerOperation(new TypedBinaryOperator.IVariantOperation<IComposite, String>() {
					@Override
					public TypedValue apply(TypeDomain domain, IComposite left, String right) {
						return left.get(domain, right);
					}
				})
				.build(domain)).unwrap();

		{
			class SpaceshipOperation<T extends Comparable<T>> implements TypedBinaryOperator.ICoercedOperation<T> {
				@Override
				public TypedValue apply(TypeDomain domain, T left, T right) {
					return domain.create(BigInteger.class, BigInteger.valueOf(left.compareTo(right)));
				}
			}

			operators.registerBinaryOperator(new TypedBinaryOperator.Builder("<=>", PRIORITY_SPACESHIP)
					.registerOperation(BigInteger.class, new SpaceshipOperation<BigInteger>())
					.registerOperation(Double.class, new SpaceshipOperation<Double>())
					.registerOperation(String.class, new SpaceshipOperation<String>())
					.registerOperation(Boolean.class, new SpaceshipOperation<Boolean>())
					.build(domain));
		}

		final BinaryOperator<TypedValue> lambdaOperator = operators.registerBinaryOperator(new MarkerBinaryOperator("->", PRIORITY_LAMBDA, Associativity.RIGHT)).unwrap();

		final BinaryOperator<TypedValue> colonOperator = operators.registerBinaryOperator(new BinaryOperator<TypedValue>(":", PRIORITY_CONS, Associativity.RIGHT) {
			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
				return domain.create(Cons.class, new Cons(left, right));
			}
		}).unwrap();

		// NOTE: this operator won't be available in prefix and postfix
		final BinaryOperator<TypedValue> defaultOperator = operators.registerDefaultOperator(new MarkerBinaryOperator("<?>", PRIORITY_MAX));

		final TypedValueParser valueParser = new TypedValueParser(domain);

		final TypedValuePrinter valuePrinter = new TypedValuePrinter(nullValue);

		final Environment<TypedValue> env = new Environment<TypedValue>(nullValue) {
			@Override
			protected Frame<TypedValue> createTopMap() {
				class TypedValueSymbolMap extends TopSymbolMap<TypedValue> {
					@Override
					protected ISymbol<TypedValue> createSymbol(ICallable<TypedValue> callable) {
						return new CallableWithValue(domain, callable);
					}

					@Override
					@SuppressWarnings("unchecked")
					protected ISymbol<TypedValue> createSymbol(TypedValue value) {
						if (value.value instanceof ICallable)
							return createSymbol((ICallable<TypedValue>)value.value);
						else
							return super.createSymbol(value);
					}

				}

				return new Frame<TypedValue>(new TypedValueSymbolMap(), new Stack<TypedValue>());
			}
		};

		GenericFunctions.createStackManipulationFunctions(env);

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_NULL, nullValue);

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_FALSE, domain.create(Boolean.class, Boolean.TRUE));
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_TRUE, domain.create(Boolean.class, Boolean.FALSE));

		env.setGlobalSymbol("E", domain.create(Double.class, Math.E));
		env.setGlobalSymbol("PI", domain.create(Double.class, Math.PI));
		env.setGlobalSymbol("NAN", domain.create(Double.class, Double.NaN));
		env.setGlobalSymbol("INF", domain.create(Double.class, Double.POSITIVE_INFINITY));

		env.setGlobalSymbol("I", domain.create(Complex.class, Complex.I));

		env.setGlobalSymbol("isint", new PredicateIsType(BigInteger.class));
		env.setGlobalSymbol("isbool", new PredicateIsType(Boolean.class));
		env.setGlobalSymbol("isfloat", new PredicateIsType(Double.class));
		env.setGlobalSymbol("isnull", new PredicateIsType(UnitType.class));
		env.setGlobalSymbol("isstr", new PredicateIsType(String.class));
		env.setGlobalSymbol("iscomplex", new PredicateIsType(Complex.class));
		env.setGlobalSymbol("isobject", new PredicateIsType(IComposite.class));
		env.setGlobalSymbol("iscons", new PredicateIsType(Cons.class));
		env.setGlobalSymbol("issymbol", new PredicateIsType(Symbol.class));
		env.setGlobalSymbol("iscode", new PredicateIsType(Code.class));
		env.setGlobalSymbol("iscallable", new PredicateIsType(ICallable.class));

		env.setGlobalSymbol("isnumber", new UnaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				return value.domain.create(Boolean.class, NUMBER_TYPES.contains(value.type));
			}
		});

		env.setGlobalSymbol("type", new UnaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				final TypeDomain domain = value.domain;
				return domain.create(String.class, domain.getName(value.type));
			}
		});

		env.setGlobalSymbol("bool", new UnaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				final Optional<Boolean> isTruthy = value.isTruthy();
				Preconditions.checkArgument(isTruthy.isPresent(), "Cannot determine value of %s", value);
				return value.domain.create(Boolean.class, isTruthy.get());
			}
		});

		env.setGlobalSymbol("str", new UnaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				if (value.is(String.class)) return value;
				if (value.is(Symbol.class)) return value.domain.create(String.class, value.as(Symbol.class).value);
				else return value.domain.create(String.class, valuePrinter.toString(value));
			}
		});

		env.setGlobalSymbol("int", new SimpleTypedFunction(domain) {
			@Variant
			public BigInteger convert(@DispatchArg(extra = { Boolean.class }) BigInteger value) {
				return value;
			}

			@Variant
			public BigInteger convert(@DispatchArg Double value) {
				return BigInteger.valueOf(value.longValue());
			}

			@Variant
			public BigInteger convert(@DispatchArg String value, @OptionalArgs Optional<BigInteger> radix) {
				final int usedRadix = radix.transform(INT_UNWRAP).or(valuePrinter.base);
				final Pair<BigInteger, Double> result = TypedValueParser.NUMBER_PARSER.parseString(value, usedRadix);
				Preconditions.checkArgument(result.getRight() == null, "Fractional part in argument to 'int': %s", value);
				return result.getLeft();
			}
		});

		env.setGlobalSymbol("float", new SimpleTypedFunction(domain) {
			@Variant
			public Double convert(@DispatchArg(extra = { BigInteger.class, Boolean.class }) Double value) {
				return value;
			}

			@Variant
			public Double convert(@DispatchArg String value, @OptionalArgs Optional<BigInteger> radix) {
				final int usedRadix = radix.transform(INT_UNWRAP).or(valuePrinter.base);
				final Pair<BigInteger, Double> result = TypedValueParser.NUMBER_PARSER.parseString(value, usedRadix);
				return result.getLeft().doubleValue() + result.getRight();
			}
		});

		env.setGlobalSymbol("complex", new SimpleTypedFunction(domain) {
			@Variant
			public Complex convert(Double re, Double im) {
				return Complex.cartesian(re, im);
			}
		});

		env.setGlobalSymbol("polar", new SimpleTypedFunction(domain) {
			@Variant
			public Complex convert(Double r, Double phase) {
				return Complex.polar(r, phase);
			}
		});

		env.setGlobalSymbol("number", new SimpleTypedFunction(domain) {
			@Variant
			@RawReturn
			public TypedValue convert(@RawDispatchArg({ Boolean.class, BigInteger.class, Double.class, Complex.class }) TypedValue value) {
				return value;
			}

			@Variant
			@RawReturn
			public TypedValue convert(@DispatchArg String value, @OptionalArgs Optional<BigInteger> radix) {
				final int usedRadix = radix.transform(INT_UNWRAP).or(valuePrinter.base);
				final Pair<BigInteger, Double> result = TypedValueParser.NUMBER_PARSER.parseString(value, usedRadix);
				return TypedValueParser.mergeNumberParts(domain, result);
			}
		});

		env.setGlobalSymbol("symbol", new SimpleTypedFunction(domain) {
			@Variant
			public Symbol symbol(String value) {
				return Symbol.get(value);
			}
		});

		env.setGlobalSymbol("parse", new SimpleTypedFunction(domain) {
			private final Tokenizer tokenizer = new Tokenizer();

			@Variant
			@RawReturn
			public TypedValue parse(String value) {
				try {
					final List<Token> tokens = Lists.newArrayList(tokenizer.tokenize(value));
					Preconditions.checkState(tokens.size() == 1, "Expected single token from '%', got %s", value, tokens.size());
					return valueParser.parseToken(tokens.get(0));
				} catch (Exception e) {
					throw new IllegalArgumentException("Failed to parse '" + value + "'", e);
				}
			}
		});

		env.setGlobalSymbol("isnan", new SimpleTypedFunction(domain) {
			@Variant
			public Boolean isNan(Double v) {
				return v.isNaN();
			}
		});

		env.setGlobalSymbol("isinf", new SimpleTypedFunction(domain) {
			@Variant
			public Boolean isInf(Double v) {
				return v.isInfinite();
			}
		});

		env.setGlobalSymbol("abs", new SimpleTypedFunction(domain) {
			@Variant
			public Boolean abs(@DispatchArg Boolean v) {
				return v;
			}

			@Variant
			public BigInteger abs(@DispatchArg BigInteger v) {
				return v.abs();
			}

			@Variant
			public Double abs(@DispatchArg Double v) {
				return Math.abs(v);
			}

			@Variant
			public Double abs(@DispatchArg Complex v) {
				return v.abs();
			}
		});

		env.setGlobalSymbol("sqrt", new SimpleTypedFunction(domain) {
			@Variant
			public Double sqrt(Double v) {
				return Math.sqrt(v);
			}
		});

		env.setGlobalSymbol("floor", new SimpleTypedFunction(domain) {
			@Variant
			@RawReturn
			public TypedValue floor(@RawDispatchArg({ BigInteger.class, Boolean.class }) TypedValue v) {
				return v;
			}

			@Variant
			public Double floor(@DispatchArg Double v) {
				return Math.floor(v);
			}
		});

		env.setGlobalSymbol("ceil", new SimpleTypedFunction(domain) {
			@Variant
			@RawReturn
			public TypedValue ceil(@RawDispatchArg({ BigInteger.class, Boolean.class }) TypedValue v) {
				return v;
			}

			@Variant
			public Double ceil(@DispatchArg Double v) {
				return Math.ceil(v);
			}
		});

		env.setGlobalSymbol("cos", new SimpleTypedFunction(domain) {
			@Variant
			public Double cos(Double v) {
				return Math.cos(v);
			}
		});

		env.setGlobalSymbol("cosh", new SimpleTypedFunction(domain) {
			@Variant
			public Double cosh(Double v) {
				return Math.cosh(v);
			}
		});

		env.setGlobalSymbol("acos", new SimpleTypedFunction(domain) {
			@Variant
			public Double acos(Double v) {
				return Math.acos(v);
			}
		});

		env.setGlobalSymbol("acosh", new SimpleTypedFunction(domain) {
			@Variant
			public Double acosh(Double v) {
				return Math.log(v + Math.sqrt(v * v - 1));
			}
		});

		env.setGlobalSymbol("sin", new SimpleTypedFunction(domain) {
			@Variant
			public Double sin(Double v) {
				return Math.sin(v);
			}
		});

		env.setGlobalSymbol("sinh", new SimpleTypedFunction(domain) {
			@Variant
			public Double sinh(Double v) {
				return Math.sinh(v);
			}
		});

		env.setGlobalSymbol("asin", new SimpleTypedFunction(domain) {
			@Variant
			public Double asin(Double v) {
				return Math.asin(v);
			}
		});

		env.setGlobalSymbol("asinh", new SimpleTypedFunction(domain) {
			@Variant
			public Double asinh(Double v) {
				return v.isInfinite()? v : Math.log(v + Math.sqrt(v * v + 1));
			}
		});

		env.setGlobalSymbol("tan", new SimpleTypedFunction(domain) {
			@Variant
			public Double tan(Double v) {
				return Math.tan(v);
			}
		});

		env.setGlobalSymbol("atan", new SimpleTypedFunction(domain) {
			@Variant
			public Double atan(Double v) {
				return Math.atan(v);
			}
		});

		env.setGlobalSymbol("atan2", new SimpleTypedFunction(domain) {
			@Variant
			public Double atan2(Double x, Double y) {
				return Math.atan2(x, y);
			}
		});

		env.setGlobalSymbol("tanh", new SimpleTypedFunction(domain) {
			@Variant
			public Double tanh(Double v) {
				return Math.tanh(v);
			}
		});

		env.setGlobalSymbol("atanh", new SimpleTypedFunction(domain) {
			@Variant
			public Double atanh(Double v) {
				return Math.log((1 + v) / (1 - v)) / 2;
			}
		});

		env.setGlobalSymbol("exp", new SimpleTypedFunction(domain) {
			@Variant
			public Double exp(@DispatchArg(extra = { Boolean.class, BigInteger.class }) Double v) {
				return Math.exp(v);
			}

			@Variant
			public Complex exp(@DispatchArg Complex v) {
				return v.exp();
			}
		});

		env.setGlobalSymbol("ln", new SimpleTypedFunction(domain) {
			@Variant
			public Double ln(@DispatchArg(extra = { Boolean.class, BigInteger.class }) Double v) {
				return Math.log(v);
			}

			@Variant
			public Complex ln(@DispatchArg Complex v) {
				return v.ln();
			}
		});

		env.setGlobalSymbol("log", new SimpleTypedFunction(domain) {
			@Variant
			public Double log(Double v, @OptionalArgs Optional<Double> base) {
				if (base.isPresent()) {
					return Math.log(v) / Math.log(base.get());
				} else {
					return Math.log10(v);
				}
			}
		});

		env.setGlobalSymbol("sgn", new SimpleTypedFunction(domain) {
			@Variant
			public BigInteger sgn(@DispatchArg(extra = { Boolean.class }) BigInteger v) {
				return BigInteger.valueOf(v.signum());
			}

			@Variant
			public Double sgn(@DispatchArg Double v) {
				return Math.signum(v);
			}
		});

		env.setGlobalSymbol("rad", new SimpleTypedFunction(domain) {
			@Variant
			public Double rad(Double v) {
				return Math.toRadians(v);
			}
		});

		env.setGlobalSymbol("deg", new SimpleTypedFunction(domain) {
			@Variant
			public Double deg(Double v) {
				return Math.toDegrees(v);
			}
		});

		env.setGlobalSymbol("modpow", new SimpleTypedFunction(domain) {
			@Variant
			public BigInteger modpow(BigInteger v, BigInteger exp, BigInteger mod) {
				return v.modPow(exp, mod);
			}
		});

		env.setGlobalSymbol("gcd", new SimpleTypedFunction(domain) {
			@Variant
			public BigInteger gcd(BigInteger v1, BigInteger v2) {
				return v1.gcd(v2);
			}
		});

		env.setGlobalSymbol("re", new SimpleTypedFunction(domain) {
			@Variant
			public Double re(@DispatchArg(extra = { Boolean.class, BigInteger.class }) Double v) {
				return v;
			}

			@Variant
			public Double re(@DispatchArg Complex v) {
				return v.re;
			}
		});

		env.setGlobalSymbol("im", new SimpleTypedFunction(domain) {
			@Variant
			public Double im(@DispatchArg(extra = { Boolean.class, BigInteger.class }) Double v) {
				return 0.0;
			}

			@Variant
			public Double im(@DispatchArg Complex v) {
				return v.im;
			}
		});

		env.setGlobalSymbol("phase", new SimpleTypedFunction(domain) {
			@Variant
			public Double phase(@DispatchArg(extra = { Boolean.class, BigInteger.class }) Double v) {
				return 0.0;
			}

			@Variant
			public Double phase(@DispatchArg Complex v) {
				return v.phase();
			}
		});

		env.setGlobalSymbol("conj", new SimpleTypedFunction(domain) {
			@Variant
			public Complex conj(@DispatchArg(extra = { Boolean.class, BigInteger.class }) Double v) {
				return Complex.real(v);
			}

			@Variant
			public Complex conj(@DispatchArg Complex v) {
				return v.conj();
			}
		});

		env.setGlobalSymbol("min", new AccumulatorFunction<TypedValue>(nullValue) {
			@Override
			protected TypedValue accumulate(TypedValue result, TypedValue value) {
				return ltOperator.execute(result, value).value == Boolean.TRUE? result : value;
			}
		});

		env.setGlobalSymbol("max", new AccumulatorFunction<TypedValue>(nullValue) {
			@Override
			protected TypedValue accumulate(TypedValue result, TypedValue value) {
				return gtOperator.execute(result, value).value == Boolean.TRUE? result : value;
			}
		});

		env.setGlobalSymbol("sum", new AccumulatorFunction<TypedValue>(nullValue) {
			@Override
			protected TypedValue accumulate(TypedValue result, TypedValue value) {
				return addOperator.execute(result, value);
			}
		});

		env.setGlobalSymbol("avg", new AccumulatorFunction<TypedValue>(nullValue) {
			@Override
			protected TypedValue accumulate(TypedValue result, TypedValue value) {
				return addOperator.execute(result, value);
			}

			@Override
			protected TypedValue process(TypedValue result, int argCount) {
				return divideOperator.execute(result, domain.create(BigInteger.class, BigInteger.valueOf(argCount)));
			}
		});

		env.setGlobalSymbol("cons", new BinaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue left, TypedValue right) {
				return domain.create(Cons.class, new Cons(left, right));
			}
		});

		env.setGlobalSymbol("car", new SimpleTypedFunction(domain) {
			@Variant
			@RawReturn
			public TypedValue car(Cons cons) {
				return cons.car;
			}
		});

		env.setGlobalSymbol("cdr", new SimpleTypedFunction(domain) {
			@Variant
			@RawReturn
			public TypedValue cdr(Cons cons) {
				return cons.cdr;
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LIST, new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				if (returnsCount.isPresent()) {
					final int returns = returnsCount.get();
					if (returns != 1) throw new StackValidationException("Has one result but expected %s", returns);
				}

				final Integer args = argumentsCount.or(0);
				final Stack<TypedValue> stack = frame.stack();

				TypedValue result = nullValue;
				for (int i = 0; i < args; i++)
					result = domain.create(Cons.class, new Cons(stack.pop(), result));

				stack.push(result);
			}
		});

		env.setGlobalSymbol("len", new SimpleTypedFunction(domain) {
			@Variant
			public BigInteger len(@DispatchArg UnitType v) {
				// since empty list == nil
				return BigInteger.ZERO;
			}

			@Variant
			public BigInteger len(@DispatchArg String v) {
				return BigInteger.valueOf(v.length());
			}

			@Variant
			public BigInteger len(@DispatchArg Cons v) {
				return BigInteger.valueOf(v.length());
			}
		});

		env.setGlobalSymbol("execute", new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				if (argumentsCount.isPresent()) {
					final int args = argumentsCount.get();
					if (args != 1) throw new StackValidationException("Expected one argument but got %s", args);
				}

				final Frame<TypedValue> sandboxFrame = FrameFactory.newProtectionFrameWithSubstack(frame, 1);
				final TypedValue top = sandboxFrame.stack().pop();
				top.as(Code.class, "first argument").execute(sandboxFrame);

				if (returnsCount.isPresent()) {
					final int expectedReturns = returnsCount.get();
					final int actualReturns = sandboxFrame.stack().size();
					if (expectedReturns != actualReturns) throw new StackValidationException("Has %s result(s) but expected %s", actualReturns, expectedReturns);
				}
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_SLICE, new SimpleTypedFunction(domain) {
			@Variant
			public String charAt(@DispatchArg String str, @DispatchArg(extra = { Boolean.class }) BigInteger index) {
				int i = index.intValue();
				if (i < 0) i = str.length() + i;
				return String.valueOf(str.charAt(i));
			}

			@Variant
			public String substr(@DispatchArg String str, @DispatchArg Cons range) {
				final int left = calculateBoundary(range.car, str.length());
				final int right = calculateBoundary(range.cdr, str.length());
				return str.substring(left, right);
			}

			private int calculateBoundary(TypedValue v, int length) {
				final int i = v.unwrap(BigInteger.class).intValue();
				return i >= 0? i : (length + i);
			}

			@Variant
			@RawReturn
			public TypedValue substr(@DispatchArg IComposite obj, String index) {
				return obj.get(domain, index);
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_APPLY, new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				Preconditions.checkArgument(argumentsCount.isPresent(), "'apply' cannot be called without argument count");
				int args = argumentsCount.get();

				TypedValue targetValue = frame.stack().drop(args - 1);
				Preconditions.checkState(targetValue.value instanceof ICallable, "Expected callable, got %s", targetValue);
				@SuppressWarnings("unchecked")
				final ICallable<TypedValue> targetCallable = (ICallable<TypedValue>)targetValue.value;
				targetCallable.call(frame, Optional.of(args - 1), returnsCount);
			}
		});

		env.setGlobalSymbol("fail", new ICallable<TypedValue>() {

			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				if (argumentsCount.isPresent()) {
					final Integer gotArgs = argumentsCount.get();
					if (gotArgs == 1) {
						final TypedValue cause = frame.stack().pop();
						throw new ExecutionErrorException(valuePrinter.toString(cause));
					}

					Preconditions.checkArgument(gotArgs == 0, "'fail' expects at most single argument, got %s", gotArgs);
				}

				throw new ExecutionErrorException();
			}

		});

		final IfExpressionFactory ifFactory = new IfExpressionFactory(domain);
		ifFactory.registerSymbol(env);

		final LetExpressionFactory letFactory = new LetExpressionFactory(domain, nullValue, colonOperator);
		letFactory.registerSymbol(env);

		final LambdaExpressionFactory lambdaFactory = new LambdaExpressionFactory(domain, nullValue);
		lambdaFactory.registerSymbol(env);

		final PromiseExpressionFactory delayFactory = new PromiseExpressionFactory(domain);
		delayFactory.registerSymbols(env);

		class TypedValueCompilersFactory extends BasicCompilerMapFactory<TypedValue> {

			@Override
			protected void configureCompilerStateCommon(MappedCompilerState<TypedValue> compilerState) {
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_QUOTE, new QuoteStateTransition.ForSymbol(domain, nullValue, valueParser));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_CODE, new CodeStateTransition(domain, compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_IF, ifFactory.createStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_LET, letFactory.createLetStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_LETSEQ, letFactory.createLetSeqStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_LETREC, letFactory.createLetRecStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_DELAY, delayFactory.createStateTransition(compilerState));

				compilerState.addStateTransition(TypedCalcConstants.MODIFIER_QUOTE, new QuoteStateTransition.ForModifier(domain, nullValue, valueParser));
				compilerState.addStateTransition(TypedCalcConstants.MODIFIER_OPERATOR_WRAP, new CallableOperatorWrapperModifierTransition(domain, operators));
			}

			@Override
			protected DefaultExprNodeFactory<TypedValue> createExprNodeFactory(IValueParser<TypedValue> valueParser) {
				return new MappedExprNodeFactory<TypedValue>(valueParser)
						.addFactory(dotOperator, new IBinaryExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
								return new DotExprNode(rightChild, leftChild, dotOperator, domain);
							}
						})
						.addFactory(lambdaOperator, lambdaFactory.createLambdaExprNodeFactory())
						.addFactory(defaultOperator, new IBinaryExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
								if (rightChild instanceof SquareBracketContainerNode) {
									// a[...]
									return new MethodCallNode(TypedCalcConstants.SYMBOL_SLICE, leftChild, rightChild);
								} else if (rightChild instanceof ArgBracketNode && !isNumericValueNode(leftChild)) {
									// (a)(...), a(...)(...)
									return new MethodCallNode(TypedCalcConstants.SYMBOL_APPLY, leftChild, rightChild);
								} else {
									// 5I
									return new BinaryOpNode<TypedValue>(multiplyOperator, leftChild, rightChild);
								}
							}
						})
						.addFactory(SquareBracketContainerNode.BRACKET_OPEN, SquareBracketContainerNode.<TypedValue> createNodeFactory())
						.addFactory(TypedCalcConstants.BRACKET_ARG_PACK, new IBracketExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(List<IExprNode<TypedValue>> children) {
								return new ArgBracketNode(children);
							}
						})
						.addFactory(TypedCalcConstants.BRACKET_CODE, new IBracketExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(List<IExprNode<TypedValue>> children) {
								return new RawCodeExprNode(domain, children);
							}
						});
			}

			@Override
			protected ITokenStreamCompiler<TypedValue> createPostfixParser(final IValueParser<TypedValue> valueParser, final OperatorDictionary<TypedValue> operators, Environment<TypedValue> env) {
				return addConstantEvaluatorState(valueParser, operators, env,
						new DefaultPostfixCompiler<TypedValue>(valueParser, operators))
								.addModifierStateProvider(TypedCalcConstants.MODIFIER_QUOTE, new IStateProvider<TypedValue>() {
									@Override
									public IPostfixCompilerState<TypedValue> createState() {
										return new QuotePostfixCompilerState(valueParser, domain);
									}
								})
								.addBracketStateProvider(TypedCalcConstants.BRACKET_CODE, new IStateProvider<TypedValue>() {
									@Override
									public IPostfixCompilerState<TypedValue> createState() {
										final IExecutableListBuilder<TypedValue> listBuilder = new DefaultExecutableListBuilder<TypedValue>(valueParser, operators);
										return new CodePostfixCompilerState(domain, listBuilder, TypedCalcConstants.BRACKET_CODE);
									}
								})
								.addModifierStateProvider(BasicCompilerMapFactory.MODIFIER_SYMBOL_GET, new IStateProvider<TypedValue>() {
									@Override
									public IPostfixCompilerState<TypedValue> createState() {
										return new CallableGetPostfixCompilerState(operators, domain);
									}
								});
			}

			@Override
			protected void setupPrefixTokenizer(Tokenizer tokenizer) {
				super.setupPrefixTokenizer(tokenizer);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_QUOTE);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_CDR);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_OPERATOR_WRAP);
			}

			@Override
			protected void setupInfixTokenizer(Tokenizer tokenizer) {
				super.setupInfixTokenizer(tokenizer);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_QUOTE);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_CDR);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_OPERATOR_WRAP);
			}

			@Override
			protected void setupPostfixTokenizer(Tokenizer tokenizer) {
				super.setupPostfixTokenizer(tokenizer);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_QUOTE);
			}
		}

		final Compilers<TypedValue, ExprType> compilers = new TypedValueCompilersFactory().create(nullValue, valueParser, operators, env);
		return new Calculator<TypedValue, ExprType>(env, compilers, valuePrinter);
	}
}
