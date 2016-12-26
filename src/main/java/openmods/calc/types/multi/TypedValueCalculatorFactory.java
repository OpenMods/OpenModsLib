package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.BinaryOperator.Associativity;
import openmods.calc.Calculator;
import openmods.calc.Compilers;
import openmods.calc.Environment;
import openmods.calc.ExecutionErrorException;
import openmods.calc.ExprType;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.GenericFunctions;
import openmods.calc.GenericFunctions.AccumulatorFunction;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalSymbolMap;
import openmods.calc.NullaryFunction;
import openmods.calc.OperatorDictionary;
import openmods.calc.SingleReturnCallable;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolMap;
import openmods.calc.TopSymbolMap;
import openmods.calc.UnaryFunction;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.BasicCompilerMapFactory;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.ConstantSymbolStateTransition;
import openmods.calc.parsing.DefaultExecutableListBuilder;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.DefaultPostfixCompiler;
import openmods.calc.parsing.DefaultPostfixCompiler.IStateProvider;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.IExecutableListBuilder;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IPostfixCompilerState;
import openmods.calc.parsing.ITokenStreamCompiler;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.MappedCompilerState;
import openmods.calc.parsing.MappedExprNodeFactory;
import openmods.calc.parsing.MappedExprNodeFactory.IBinaryExprNodeFactory;
import openmods.calc.parsing.MappedExprNodeFactory.IBracketExprNodeFactory;
import openmods.calc.parsing.MappedExprNodeFactory.IUnaryExprNodeFactory;
import openmods.calc.parsing.SquareBracketContainerNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.Tokenizer;
import openmods.calc.parsing.UnaryOpNode;
import openmods.calc.parsing.ValueNode;
import openmods.calc.types.multi.Cons.LinearVisitor;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.OptionalArgs;
import openmods.calc.types.multi.TypedFunction.RawDispatchArg;
import openmods.calc.types.multi.TypedFunction.RawReturn;
import openmods.calc.types.multi.TypedFunction.Variant;
import openmods.math.Complex;
import openmods.utils.OptionalInt;
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

	private static final int PRIORITY_MAX = 180; // basically magic
	private static final int PRIORITY_NULL_AWARE = 175; // ??
	private static final int PRIORITY_EXP = 170; // **
	private static final int PRIORITY_MULTIPLY = 160; // * / % //
	private static final int PRIORITY_ADD = 150; // + -
	private static final int PRIORITY_BITSHIFT = 140; // << >>
	private static final int PRIORITY_BITWISE_AND = 130; // &
	private static final int PRIORITY_BITWISE_XOR = 120; // ^
	private static final int PRIORITY_BITWISE_OR = 110; // |
	private static final int PRIORITY_COMPARE = 100; // < > <= >= <=>
	private static final int PRIORITY_SPACESHIP = 90; // <=>
	private static final int PRIORITY_EQUALS = 80; // == !=
	private static final int PRIORITY_LOGIC_AND = 70; // &&
	private static final int PRIORITY_LOGIC_XOR = 60; // ||
	private static final int PRIORITY_LOGIC_OR = 50; // ^^
	private static final int PRIORITY_CONS = 40; // :
	private static final int PRIORITY_LAMBDA = 30; // ->
	private static final int PRIORITY_SPLIT = 20; // \
	private static final int PRIORITY_ASSIGN = 10; // =

	private static class MarkerUnaryOperator extends UnaryOperator.Direct<TypedValue> {
		public MarkerUnaryOperator(String id) {
			super(id);
		}

		@Override
		public TypedValue execute(TypedValue value) {
			throw new UnsupportedOperationException(); // should be replaced in AST tree modification
		}
	}

	private static class MarkerUnaryOperatorNodeFactory implements IUnaryExprNodeFactory<TypedValue> {
		private final UnaryOperator<TypedValue> op;

		public MarkerUnaryOperatorNodeFactory(UnaryOperator<TypedValue> op) {
			this.op = op;
		}

		@Override
		public IExprNode<TypedValue> create(IExprNode<TypedValue> arg) {
			return new UnaryOpNode<TypedValue>(op, arg) {
				@Override
				public void flatten(List<IExecutable<TypedValue>> output) {
					throw new UnsupportedOperationException("Operator " + op + " cannot be used in this context"); // should be captured before serialization;
				}
			};
		}
	}

	private static class MarkerBinaryOperator extends BinaryOperator.Direct<TypedValue> {
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

	private static class MarkerBinaryOperatorNodeFactory implements IBinaryExprNodeFactory<TypedValue> {
		private final BinaryOperator<TypedValue> op;

		public MarkerBinaryOperatorNodeFactory(BinaryOperator<TypedValue> op) {
			this.op = op;
		}

		@Override
		public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
			return new BinaryOpNode<TypedValue>(op, leftChild, rightChild) {
				@Override
				public void flatten(List<IExecutable<TypedValue>> output) {
					throw new UnsupportedOperationException("Operator " + op + " cannot be used in this context"); // should be captured before serialization;
				}
			};
		}
	}

	private static class SingularBinaryOperatorNodeFactory implements IBinaryExprNodeFactory<TypedValue> {

		private final BinaryOperator<TypedValue> op;

		public SingularBinaryOperatorNodeFactory(BinaryOperator<TypedValue> op) {
			this.op = op;
		}

		@Override
		public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
			checkChild(leftChild);
			checkChild(rightChild);
			return new BinaryOpNode<TypedValue>(op, leftChild, rightChild);
		}

		private void checkChild(IExprNode<TypedValue> child) {
			if (child instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> childOpNode = (BinaryOpNode<TypedValue>)child;
				if (childOpNode.operator == this.op)
					throw new UnsupportedOperationException("Operator " + op.id + "(" + op + ") cannot be chained");
			}
		}
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

	private static List<Object> consToUnwrappedList(Cons pair, final TypedValue expectedTerminator) {
		final List<Object> result = Lists.newArrayList();
		pair.visit(new LinearVisitor() {

			@Override
			public void value(TypedValue value, boolean isLast) {
				result.add(value.value);
			}

			@Override
			public void end(TypedValue terminator) {
				if (terminator != expectedTerminator) throw new IllegalArgumentException("Not null terminated list");
			}

			@Override
			public void begin() {}
		});

		return result;
	}

	private static class EnvHolder {
		public final SymbolMap<TypedValue> symbols;

		public EnvHolder(SymbolMap<TypedValue> symbols) {
			this.symbols = symbols;
		}
	}

	private static abstract class SlotCaller extends FixedCallable<TypedValue> {

		public SlotCaller() {
			super(1, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final TypedValue value = frame.stack().pop();
			frame.stack().push(callSlot(frame, value, value.getMetaObject()));
		}

		protected abstract TypedValue callSlot(Frame<TypedValue> frame, TypedValue value, MetaObject metaObject);
	}

	public static Calculator<TypedValue, ExprType> create() {
		final TypeDomain domain = new TypeDomain(MetaObject.builder().set(MetaObjectUtils.USE_VALUE_EQUALS).build());

		final TypedValueParser valueParser = new TypedValueParser(domain);

		domain.registerType(TypeUserdata.class, "type", TypeUserdata.defaultMetaObject(domain).build());

		SimpleNamespace.register(domain);

		{

			final TypedValue nullType = domain.create(TypeUserdata.class, new TypeUserdata("<null>", UnitType.class));

			domain.registerType(UnitType.class, "<null>",
					MetaObject.builder()
							.set(MetaObjectUtils.BOOL_ALWAYS_FALSE)
							.set(new MetaObject.SlotLength() {
								@Override
								public int length(TypedValue self, Frame<TypedValue> frame) {
									return 0;
								}
							})
							.set(MetaObjectUtils.strConst("null"))
							.set(MetaObjectUtils.reprConst("null"))
							.set(MetaObjectUtils.typeConst(nullType))
							.build());
		}

		final TypedValue nullValue = domain.create(UnitType.class, UnitType.INSTANCE);
		final TypedValuePrinter valuePrinter = new TypedValuePrinter(nullValue);

		final Map<String, TypedValue> basicTypes = Maps.newHashMap();

		{
			final TypedValue intType = domain.create(TypeUserdata.class, new TypeUserdata("int", BigInteger.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new SimpleTypedFunction(domain) {
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
							}))
							.build());

			basicTypes.put("int", intType);

			domain.registerType(BigInteger.class, "int",
					MetaObject.builder()
							.set(new MetaObject.SlotBool() {
								@Override
								public boolean bool(TypedValue value, Frame<TypedValue> frame) {
									return !value.as(BigInteger.class).equals(BigInteger.ZERO);
								}
							})
							.set(MetaObjectUtils.typeConst(intType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(BigInteger.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(BigInteger.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{
			final TypedValue floatType = domain.create(TypeUserdata.class, new TypeUserdata("float", Double.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new SimpleTypedFunction(domain) {
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
							}))
							.build());

			basicTypes.put("float", floatType);

			domain.registerType(Double.class, "float",
					MetaObject.builder()
							.set(new MetaObject.SlotBool() {
								@Override
								public boolean bool(TypedValue value, Frame<TypedValue> frame) {
									return value.as(Double.class) != 0;
								}
							})
							.set(MetaObjectUtils.typeConst(floatType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(Double.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(Double.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{
			final TypedValue boolType = domain.create(TypeUserdata.class, new TypeUserdata("bool", Boolean.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new SlotCaller() {
								@Override
								protected TypedValue callSlot(Frame<TypedValue> frame, TypedValue value, MetaObject metaObject) {
									return domain.create(Boolean.class, MetaObjectUtils.boolValue(frame, value));
								}
							}))
							.build());

			basicTypes.put("bool", boolType);

			domain.registerType(Boolean.class, "bool",
					MetaObject.builder()
							.set(new MetaObject.SlotBool() {
								@Override
								public boolean bool(TypedValue value, Frame<TypedValue> frame) {
									return value.as(Boolean.class).booleanValue();
								}
							})
							.set(MetaObjectUtils.typeConst(boolType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(Boolean.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(Boolean.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{
			final TypedValue strType = domain.create(TypeUserdata.class, new TypeUserdata("str", String.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new UnaryFunction.Direct<TypedValue>() {
								@Override
								protected TypedValue call(TypedValue value) {
									return value.domain.create(String.class, valuePrinter.str(value));
								}
							}))
							.build());

			basicTypes.put("str", strType);

			domain.registerType(String.class, "str",
					MetaObject.builder()
							.set(new MetaObject.SlotLength() {
								@Override
								public int length(TypedValue self, Frame<TypedValue> frame) {
									return self.as(String.class).length();
								}
							})
							.set(new MetaObject.SlotSlice() {
								@Override
								public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
									final String target = self.as(String.class);
									final String result;

									if (range.is(Cons.class)) {
										result = substr(target, range.as(Cons.class));
									} else {
										result = charAt(target, range.unwrap(BigInteger.class));
									}

									return domain.create(String.class, result);
								}

								private String substr(String str, Cons range) {
									final int left = calculateBoundary(range.car, str.length());
									final int right = calculateBoundary(range.cdr, str.length());
									return str.substring(left, right);
								}

								private int calculateBoundary(TypedValue v, int length) {
									final int i = v.unwrap(BigInteger.class).intValue();
									return i >= 0? i : (length + i);
								}

								private String charAt(String str, BigInteger index) {
									int i = index.intValue();
									if (i < 0) i = str.length() + i;
									return String.valueOf(str.charAt(i));
								}

							})
							.set(new MetaObject.SlotBool() {
								@Override
								public boolean bool(TypedValue value, Frame<TypedValue> frame) {
									return !value.as(String.class).isEmpty();
								}
							})
							.set(MetaObjectUtils.typeConst(strType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(String.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(String.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{
			final TypedValue complexType = domain.create(TypeUserdata.class, new TypeUserdata("complex", Complex.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new SimpleTypedFunction(domain) {
								@Variant
								public Complex convert(Double re, Double im) {
									return Complex.cartesian(re, im);
								}
							}))
							.build());

			basicTypes.put("complex", complexType);

			domain.registerType(Complex.class, "complex",
					MetaObject.builder()
							.set(new MetaObject.SlotBool() {
								@Override
								public boolean bool(TypedValue value, Frame<TypedValue> frame) {
									return !value.as(Complex.class).equals(Complex.ZERO);
								}
							})
							.set(MetaObjectUtils.typeConst(complexType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(Complex.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(Complex.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{
			final TypedValue consType = domain.create(TypeUserdata.class, new TypeUserdata("cons", Cons.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new BinaryFunction.Direct<TypedValue>() {
								@Override
								protected TypedValue call(TypedValue left, TypedValue right) {
									return domain.create(Cons.class, new Cons(left, right));
								}
							}))
							.build());

			basicTypes.put("cons", consType);

			domain.registerType(Cons.class, "cons",
					MetaObject.builder()
							.set(new MetaObject.SlotLength() {
								@Override
								public int length(TypedValue self, Frame<TypedValue> frame) {
									return self.as(Cons.class).length();
								}
							})
							.set(MetaObjectUtils.BOOL_ALWAYS_TRUE)
							.set(MetaObjectUtils.typeConst(consType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(Cons.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(Cons.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{
			final TypedValue symbolType = domain.create(TypeUserdata.class, new TypeUserdata("symbol", Symbol.class),
					TypeUserdata.defaultMetaObject(domain)
							.set(MetaObjectUtils.callableAdapter(new SimpleTypedFunction(domain) {
								@Variant
								public Symbol symbol(String value) {
									return Symbol.get(value);
								}
							}))
							.build());

			basicTypes.put("symbol", symbolType);

			domain.registerType(Symbol.class, "symbol",
					MetaObject.builder()
							.set(MetaObjectUtils.BOOL_ALWAYS_TRUE)
							.set(MetaObjectUtils.typeConst(symbolType))
							.set(new MetaObject.SlotStr() {
								@Override
								public String str(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.str(self.as(Symbol.class));
								}
							})
							.set(new MetaObject.SlotRepr() {
								@Override
								public String repr(TypedValue self, Frame<TypedValue> frame) {
									return valuePrinter.repr(self.as(Symbol.class));
								}
							})
							.set(MetaObjectUtils.USE_VALUE_EQUALS)
							.build());
		}

		{

			final TypedValue functionType = domain.create(TypeUserdata.class, new TypeUserdata("function", CallableValue.class));

			basicTypes.put("function", functionType);

			domain.registerType(CallableValue.class, "function",
					MetaObject.builder()
							.set(new MetaObject.SlotCall() {
								@Override
								public void call(TypedValue self, OptionalInt argCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
									final CallableValue function = self.as(CallableValue.class);
									function.call(self, argCount, returnsCount, frame);
								}
							})
							.set(MetaObjectUtils.BOOL_ALWAYS_TRUE)
							.set(MetaObjectUtils.typeConst(functionType))
							.build());
		}

		{
			final TypedValue envType = domain.create(TypeUserdata.class, new TypeUserdata("env", EnvHolder.class));

			basicTypes.put("env", envType);

			domain.registerType(EnvHolder.class, "env",
					MetaObject.builder()
							.set(new MetaObject.SlotAttr() {
								@Override
								public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
									final EnvHolder target = self.as(EnvHolder.class);
									final ISymbol<TypedValue> result = target.symbols.get(key);
									if (result == null) return Optional.absent();
									return Optional.of(result.get());
								}
							})
							.set(MetaObjectUtils.typeConst(envType))
							.build());

		}

		{
			// TODO decompose may not work due to special 'code' form
			final TypedValue codeType = domain.create(TypeUserdata.class, new TypeUserdata("code", Code.class));

			basicTypes.put("code", codeType);

			domain.registerType(Code.class, "code",
					MetaObject.builder()
							.set(MetaObjectUtils.BOOL_ALWAYS_TRUE)
							.set(MetaObjectUtils.typeConst(codeType))
							.build());
		}

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

		final OperatorDictionary<TypedValue> operators = new OperatorDictionary<TypedValue>();

		// arithmetic
		final BinaryOperator.Direct<TypedValue> addOperator = operators.registerBinaryOperator(new TypedBinaryOperator.Builder("+", PRIORITY_ADD)
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

		operators.registerUnaryOperator(new UnaryOperator.Direct<TypedValue>("+") {
			@Override
			public TypedValue execute(TypedValue value) {
				Preconditions.checkState(NUMBER_TYPES.contains(value.type), "Not a number: %s", value);
				return value;
			}
		});

		final UnaryOperator<TypedValue> varArgMarker = operators.registerUnaryOperator(new MarkerUnaryOperator("*"));

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

		final BinaryOperator.Direct<TypedValue> divideOperator = operators.registerBinaryOperator(new TypedBinaryOperator.Builder("/", PRIORITY_MULTIPLY)
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
				.setDefaultOperation(new TypedBinaryOperator.IDefaultOperation() {
					@Override
					public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right) {
						if (!left.is(String.class)) return Optional.absent();
						final String template = left.as(String.class);
						final Object[] args = right.is(Cons.class)
								? consToUnwrappedList(right.as(Cons.class), nullValue).toArray()
								: new Object[] { right.value };
						final String result = String.format(template, args);
						return Optional.of(domain.create(String.class, result));
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

		operators.registerUnaryOperator(new UnaryOperator.StackBased<TypedValue>("!") {
			@Override
			public void executeOnStack(Frame<TypedValue> frame) {
				final TypedValue selfValue = frame.stack().pop();
				final boolean boolValue = MetaObjectUtils.boolValue(frame, selfValue);
				frame.stack().push(domain.create(Boolean.class, !boolValue));
			}
		});

		abstract class BinaryLogicOperator extends BinaryOperator.StackBased<TypedValue> {

			public BinaryLogicOperator(String id, int precendence) {
				super(id, precendence);
			}

			@Override
			public void executeOnStack(Frame<TypedValue> frame) {
				final Stack<TypedValue> stack = frame.stack();
				final TypedValue right = stack.pop();
				final TypedValue left = stack.pop();
				final Frame<TypedValue> truthFrame = FrameFactory.newLocalFrame(frame);
				truthFrame.stack().push(left);
				final boolean flag = MetaObjectUtils.boolValue(frame, left);
				stack.push(select(flag, left, right));
			}

			protected abstract TypedValue select(boolean firstBool, TypedValue first, TypedValue second);
		}

		final BinaryOperator<TypedValue> andOperator = operators.registerBinaryOperator(new BinaryLogicOperator("&&", PRIORITY_LOGIC_AND) {
			@Override
			public TypedValue select(boolean firstBool, TypedValue left, TypedValue right) {
				Preconditions.checkArgument(left.domain == right.domain, "Values from different domains: %s, %s", left, right);
				return firstBool? right : left;
			}
		}).unwrap();

		final BinaryOperator<TypedValue> orOperator = operators.registerBinaryOperator(new BinaryLogicOperator("||", PRIORITY_LOGIC_OR) {
			@Override
			public TypedValue select(boolean firstBool, TypedValue left, TypedValue right) {
				Preconditions.checkArgument(left.domain == right.domain, "Values from different domains: %s, %s", left, right);
				return firstBool? left : right;
			}
		}).unwrap();

		operators.registerBinaryOperator(new BinaryOperator.StackBased<TypedValue>("^^", PRIORITY_LOGIC_XOR) {

			@Override
			public void executeOnStack(Frame<TypedValue> frame) {
				final Stack<TypedValue> stack = frame.stack();
				final TypedValue right = stack.peek(0);
				final Boolean rightValue = MetaObjectUtils.boolValue(frame, right);

				final TypedValue left = stack.peek(0);
				final Boolean leftValue = MetaObjectUtils.boolValue(frame, left);

				stack.push(left.domain.create(Boolean.class, leftValue ^ rightValue));
			}
		});

		final BinaryOperator<TypedValue> nonNullOperator = operators.registerBinaryOperator(new BinaryOperator.Direct<TypedValue>("??", PRIORITY_NULL_AWARE) {
			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
				return left != nullValue? left : right;
			}
		}).unwrap();

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

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("&", PRIORITY_BITWISE_AND)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, Boolean>() {
					@Override
					public Boolean apply(Boolean left, Boolean right) {
						return left & right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.and(right);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("|", PRIORITY_BITWISE_OR)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, Boolean>() {
					@Override
					public Boolean apply(Boolean left, Boolean right) {
						return left | right;
					}
				})
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<BigInteger, BigInteger>() {
					@Override
					public BigInteger apply(BigInteger left, BigInteger right) {
						return left.or(right);
					}
				})
				.build(domain));

		operators.registerBinaryOperator(new TypedBinaryOperator.Builder("^", PRIORITY_BITWISE_XOR)
				.registerOperation(new TypedBinaryOperator.ISimpleCoercedOperation<Boolean, Boolean>() {
					@Override
					public Boolean apply(Boolean left, Boolean right) {
						return left ^ right;
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

		// comparison

		final TypedValueComparator comparator = new TypedValueComparator();

		abstract class BooleanComparatorOperator extends BinaryOperator.Direct<TypedValue> {

			public BooleanComparatorOperator(String id, int precendence) {
				super(id, precendence);
			}

			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
				final int result = comparator.compare(left, right);
				return domain.create(Boolean.class, interpret(result));
			}

			protected abstract boolean interpret(int value);

		}

		final BinaryOperator.Direct<TypedValue> ltOperator = operators.registerBinaryOperator(new BooleanComparatorOperator("<", PRIORITY_COMPARE) {
			@Override
			protected boolean interpret(int value) {
				return value < 0;
			}
		}).unwrap();

		final BinaryOperator.Direct<TypedValue> gtOperator = operators.registerBinaryOperator(new BooleanComparatorOperator(">", PRIORITY_COMPARE) {
			@Override
			protected boolean interpret(int value) {
				return value > 0;
			}
		}).unwrap();

		abstract class EqualsOperator extends BinaryOperator.StackBased<TypedValue> {

			public EqualsOperator(String id, int precendence) {
				super(id, precendence);
			}

			@Override
			public void executeOnStack(Frame<TypedValue> frame) {
				final Stack<TypedValue> stack = frame.stack();
				final TypedValue right = stack.pop();
				final TypedValue left = stack.pop();
				final boolean result = TypedCalcUtils.isEqual(frame, left, right);
				stack.push(domain.create(Boolean.class, interpret(result)));
			}

			public abstract boolean interpret(boolean isEqual);
		}

		operators.registerBinaryOperator(new EqualsOperator("==", PRIORITY_EQUALS) {
			@Override
			public boolean interpret(boolean isEqual) {
				return isEqual;
			}
		});

		operators.registerBinaryOperator(new EqualsOperator("!=", PRIORITY_EQUALS) {
			@Override
			public boolean interpret(boolean isEqual) {
				return !isEqual;
			}
		});

		operators.registerBinaryOperator(new BooleanComparatorOperator("<=", PRIORITY_COMPARE) {
			@Override
			public boolean interpret(int value) {
				return value <= 0;
			}
		});

		operators.registerBinaryOperator(new BooleanComparatorOperator(">=", PRIORITY_COMPARE) {
			@Override
			public boolean interpret(int value) {
				return value >= 0;
			}
		});

		// magic

		class DotOperator extends BinaryOperator.StackBased<TypedValue> {

			public DotOperator(String id, int precendence) {
				super(id, precendence);
			}

			@Override
			public void executeOnStack(Frame<TypedValue> frame) {
				final String key = frame.stack().pop().as(String.class, "attribute name");
				final TypedValue target = frame.stack().pop();
				final MetaObject.SlotAttr slotAttr = target.getMetaObject().slotAttr;
				Preconditions.checkState(slotAttr != null, "Value %s has no attributes", target);
				final Optional<TypedValue> attr = slotAttr.attr(target, key, frame);
				Preconditions.checkState(attr.isPresent(), "Value '%s' has no attribute '%s'", target, key);
				frame.stack().push(attr.get());
			}
		}

		final BinaryOperator<TypedValue> dotOperator = operators.registerBinaryOperator(new DotOperator(".", PRIORITY_MAX)).unwrap();

		final BinaryOperator<TypedValue> nullAwareDotOperator = operators.registerBinaryOperator(new DotOperator("?.", PRIORITY_MAX)).unwrap();

		operators.registerBinaryOperator(new BinaryOperator.Direct<TypedValue>("<=>", PRIORITY_SPACESHIP) {
			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
				return domain.create(BigInteger.class, BigInteger.valueOf(comparator.compare(left, right)));
			}
		});

		final BinaryOperator<TypedValue> lambdaOperator = operators.registerBinaryOperator(new MarkerBinaryOperator("->", PRIORITY_LAMBDA, Associativity.RIGHT)).unwrap();

		final BinaryOperator<TypedValue> splitOperator = operators.registerBinaryOperator(new MarkerBinaryOperator("\\", PRIORITY_SPLIT, Associativity.RIGHT)).unwrap();

		final BinaryOperator<TypedValue> colonOperator = operators.registerBinaryOperator(new BinaryOperator.Direct<TypedValue>(":", PRIORITY_CONS, Associativity.RIGHT) {
			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
				return domain.create(Cons.class, new Cons(left, right));
			}
		}).unwrap();

		// this is just sugar - but low priority op is handy for functions that use pairs as named args
		final BinaryOperator<TypedValue> assignOperator = operators.registerBinaryOperator(new BinaryOperator.Direct<TypedValue>("=", PRIORITY_ASSIGN) {
			@Override
			public TypedValue execute(TypedValue left, TypedValue right) {
				return domain.create(Cons.class, new Cons(left, right));
			}
		}).unwrap();

		// NOTE: this operator won't be available in prefix and postfix
		final BinaryOperator<TypedValue> defaultOperator = operators.registerDefaultOperator(new MarkerBinaryOperator("<?>", PRIORITY_MAX));

		final BinaryOperator<TypedValue> nullAwareOperator = operators.registerBinaryOperator(new MarkerBinaryOperator("?", PRIORITY_MAX)).unwrap();

		class TypedValueSymbolMap extends TopSymbolMap<TypedValue> {
			@Override
			protected ISymbol<TypedValue> createSymbol(ICallable<TypedValue> callable) {
				return CallableValue.from(callable).toSymbol(domain);
			}

			@Override
			protected ISymbol<TypedValue> createSymbol(TypedValue value) {
				if (value.is(CallableValue.class))
					return value.as(CallableValue.class).toSymbol(value);

				if (MetaObjectUtils.isCallable(value)) return new SlotCallableValueSymbol(value);

				return super.createSymbol(value);
			}

		}

		final SymbolMap<TypedValue> coreMap = new TypedValueSymbolMap();

		coreMap.put(TypedCalcConstants.SYMBOL_NULL, nullValue);
		coreMap.put(TypedCalcConstants.SYMBOL_FALSE, domain.create(Boolean.class, Boolean.TRUE));
		coreMap.put(TypedCalcConstants.SYMBOL_TRUE, domain.create(Boolean.class, Boolean.FALSE));
		coreMap.put("NAN", domain.create(Double.class, Double.NaN));
		coreMap.put("INF", domain.create(Double.class, Double.POSITIVE_INFINITY));
		coreMap.put("I", domain.create(Complex.class, Complex.I));

		final Environment<TypedValue> env = new Environment<TypedValue>(nullValue) {
			@Override
			protected Frame<TypedValue> createTopMap() {
				final SymbolMap<TypedValue> mainSymbolMap = new LocalSymbolMap<TypedValue>(coreMap);
				return new Frame<TypedValue>(mainSymbolMap, new Stack<TypedValue>());
			}
		};

		final SymbolMap<TypedValue> envMap = env.topFrame().symbols();

		for (Map.Entry<String, TypedValue> e : basicTypes.entrySet())
			envMap.put(e.getKey(), e.getValue());

		GenericFunctions.createStackManipulationFunctions(env);

		env.setGlobalSymbol("E", domain.create(Double.class, Math.E));
		env.setGlobalSymbol("PI", domain.create(Double.class, Math.PI));

		env.setGlobalSymbol("iscallable", new UnaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				return value.domain.create(Boolean.class, MetaObjectUtils.isCallable(value));
			}
		});

		env.setGlobalSymbol("isnumber", new UnaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				return value.domain.create(Boolean.class, NUMBER_TYPES.contains(value.type));
			}
		});

		env.setGlobalSymbol("type", new SlotCaller() {
			@Override
			protected TypedValue callSlot(Frame<TypedValue> frame, TypedValue value, MetaObject metaObject) {
				final MetaObject.SlotType slotType = value.getMetaObject().slotType;
				return slotType != null? slotType.type(value, frame) : nullValue;
			}
		});

		env.setGlobalSymbol("repr", new UnaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				return value.domain.create(String.class, valuePrinter.repr(value));
			}
		});

		{
			final TypedValue polar = domain.create(CallableValue.class, CallableValue.from(new SimpleTypedFunction(domain) {
				@Variant
				public Complex convert(Double r, Double phase) {
					return Complex.polar(r, phase);
				}
			}),
					MetaObject.builder()
							.set(new MetaObject.SlotDecompose() {
								@Override
								public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
									Preconditions.checkState(variableCount == 2, "'polar' can provide 2 values, but code expects %s", variableCount);
									if (input.is(Complex.class)) {
										final Complex value = input.as(Complex.class);
										final TypedValue abs = domain.create(Double.class, value.abs());
										final TypedValue theta = domain.create(Double.class, value.phase());
										final List<TypedValue> result = ImmutableList.of(abs, theta);
										return Optional.of(result);
									}

									return Optional.absent();
								}
							})
							.update(domain.getDefaultMetaObject(CallableValue.class)));

			env.setGlobalSymbol("polar", polar);
		}

		{
			final TypedValue cartesian = domain.create(CallableValue.class, CallableValue.from(new SimpleTypedFunction(domain) {
				@Variant
				public Complex convert(Double x, Double y) {
					return Complex.cartesian(x, y);
				}
			}),
					MetaObject.builder()
							.set(new MetaObject.SlotDecompose() {
								@Override
								public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
									Preconditions.checkState(variableCount == 2, "'cartesian' can provide 2 values, but code expects %s", variableCount);
									if (input.is(Complex.class)) {
										final Complex value = input.as(Complex.class);
										final TypedValue x = domain.create(Double.class, value.re);
										final TypedValue y = domain.create(Double.class, value.im);
										final List<TypedValue> result = ImmutableList.of(x, y);
										return Optional.of(result);
									}

									return Optional.absent();
								}
							})
							.update(domain.getDefaultMetaObject(CallableValue.class)));

			env.setGlobalSymbol("cartesian", cartesian);
		}

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

		coreMap.put(TypedCalcConstants.SYMBOL_LIST, new SingleReturnCallable<TypedValue>() {
			@Override
			public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
				final Integer args = argumentsCount.or(0);
				final Stack<TypedValue> stack = frame.stack();

				TypedValue result = nullValue;
				for (int i = 0; i < args; i++)
					result = domain.create(Cons.class, new Cons(stack.pop(), result));

				return result;
			}
		});

		env.setGlobalSymbol("len", new SlotCaller() {
			@Override
			protected TypedValue callSlot(Frame<TypedValue> frame, TypedValue value, MetaObject metaObject) {
				final MetaObject.SlotLength slotLength = metaObject.slotLength;
				Preconditions.checkState(slotLength != null, "Value %s has no length", value);
				return domain.create(BigInteger.class, BigInteger.valueOf(slotLength.length(value, frame)));
			}
		});

		env.setGlobalSymbol("execute", new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
				TypedCalcUtils.expectExactArgCount(argumentsCount, 1);

				final Frame<TypedValue> sandboxFrame = FrameFactory.newProtectionFrameWithSubstack(frame, 1);
				final TypedValue top = sandboxFrame.stack().pop();
				top.as(Code.class, "first argument").execute(sandboxFrame);

				TypedCalcUtils.expectExactReturnCount(returnsCount, sandboxFrame.stack().size());
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_SLICE, new BinaryFunction.WithFrame<TypedValue>() {

			@Override
			public TypedValue call(Frame<TypedValue> frame, TypedValue target, TypedValue range) {
				final MetaObject.SlotSlice slotSlice = target.getMetaObject().slotSlice;
				if (slotSlice != null)
					return slotSlice.slice(target, range, frame);

				if (range.is(String.class)) {
					final MetaObject.SlotAttr slotAttr = target.getMetaObject().slotAttr;
					if (slotAttr != null) {
						final Optional<TypedValue> attr = slotAttr.attr(target, range.as(String.class), frame);
						if (attr.isPresent())
							return attr.get();
					}
				}

				throw new IllegalArgumentException("Cannot 'apply' slice operator to " + target + " for key " + range);
			}
		});

		coreMap.put(TypedCalcConstants.SYMBOL_APPLY, new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
				Preconditions.checkArgument(argumentsCount.isPresent(), "'apply' cannot be called without argument count");
				final int args = argumentsCount.get();
				Preconditions.checkArgument(args >= 1, "'apply' requires at least one argument");

				final TypedValue target = frame.stack().drop(args - 1);
				try {
					MetaObjectUtils.call(frame, target, OptionalInt.of(args - 1), returnsCount);
				} catch (Exception e) {
					throw new RuntimeException("Failed to execute value " + target, e);
				}
			}
		});

		env.setGlobalSymbol("fail", new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
				if (argumentsCount.isPresent()) {
					final Integer gotArgs = argumentsCount.get();
					if (gotArgs == 1) {
						final TypedValue cause = frame.stack().pop();
						throw new ExecutionErrorException(valuePrinter.repr(cause));
					}

					Preconditions.checkArgument(gotArgs == 0, "'fail' expects at most single argument, got %s", gotArgs);
				}

				throw new ExecutionErrorException();
			}

		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_WITH, new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
				TypedCalcUtils.expectExactArgCount(argumentsCount, 2);

				final TypedValue code = frame.stack().pop();
				code.checkType(Code.class, "Second(code) 'with' parameter");

				final TypedValue target = frame.stack().pop();

				final CompositeSymbolMap symbolMap = new CompositeSymbolMap(frame.symbols(), target);
				final Frame<TypedValue> newFrame = FrameFactory.newClosureFrame(symbolMap, frame, 0);

				code.as(Code.class).execute(newFrame);

				TypedCalcUtils.expectExactReturnCount(returnsCount, newFrame.stack().size());
			}

		});

		env.setGlobalSymbol("and", new LogicFunction.Eager(nullValue) {
			@Override
			protected boolean shouldReturn(Frame<TypedValue> scratch, TypedValue arg) {
				return !MetaObjectUtils.boolValue(scratch, arg);
			}
		});

		env.setGlobalSymbol("or", new LogicFunction.Eager(nullValue) {
			@Override
			protected boolean shouldReturn(Frame<TypedValue> scratch, TypedValue arg) {
				return MetaObjectUtils.boolValue(scratch, arg);
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_AND_THEN, new LogicFunction.Shorting(nullValue) {
			@Override
			protected boolean shouldReturn(Frame<TypedValue> scratch, TypedValue arg) {
				return !MetaObjectUtils.boolValue(scratch, arg);
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_OR_ELSE, new LogicFunction.Shorting(nullValue) {
			@Override
			protected boolean shouldReturn(Frame<TypedValue> scratch, TypedValue arg) {
				return MetaObjectUtils.boolValue(scratch, arg);
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_NON_NULL, new LogicFunction.Shorting(nullValue) {
			@Override
			public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
				final TypedValue result = super.call(frame, argumentsCount);
				Preconditions.checkState(result != nullValue, "Returning null value from 'nonnull'");
				return result;
			}

			@Override
			protected boolean shouldReturn(Frame<TypedValue> scratch, TypedValue arg) {
				return arg != nullValue;
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_NULL_EXECUTE, new FixedCallable<TypedValue>(2, 1) {
			@Override
			public void call(Frame<TypedValue> frame) {
				final Stack<TypedValue> stack = frame.stack();
				final Code op = stack.pop().as(Code.class, "second nexecute arg");
				final TypedValue value = stack.peek(0);

				if (value != nullValue) {
					final Frame<TypedValue> executionFrame = FrameFactory.newProtectionFrameWithSubstack(frame, 1);
					op.execute(executionFrame);
					final int actualReturns = executionFrame.stack().size();
					if (actualReturns != 1) throw new StackValidationException("Code must have one return, but returned %s", actualReturns);
				}
			}
		});

		OptionalType.register(env, nullValue);

		env.setGlobalSymbol("struct", new StructSymbol(nullValue));
		env.setGlobalSymbol("dict", new DictSymbol(nullValue).value());

		env.setGlobalSymbol("globals", new NullaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call() {
				return domain.create(EnvHolder.class, new EnvHolder(envMap));
			}
		});

		env.setGlobalSymbol("locals", new FixedCallable<TypedValue>(0, 1) {
			@Override
			public void call(Frame<TypedValue> frame) {
				frame.stack().push(domain.create(EnvHolder.class, new EnvHolder(frame.symbols())));
			}
		});

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_APPLYVAR, new ICallable<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
				Preconditions.checkArgument(argumentsCount.isPresent(), "'applyvar' cannot be called without argument count");
				final int args = argumentsCount.get();
				Preconditions.checkArgument(args >= 2, "'applyvar' requires at least two args");

				final Stack<TypedValue> stack = frame.stack();
				final TypedValue varArgs = stack.pop();
				final TypedValue target = stack.drop(args - 1 - 1);

				int totalArgs = args - 1 - 1; // stack.size()
				for (TypedValue varArg : Cons.toIterable(varArgs, nullValue)) {
					stack.push(varArg);
					totalArgs++;
				}

				try {
					MetaObjectUtils.call(frame, target, OptionalInt.of(totalArgs), returnsCount);
				} catch (Exception e) {
					throw new RuntimeException("Failed to execute value " + target, e);
				}
			}
		});

		env.setGlobalSymbol("random", new LibRandom(domain).type());
		LibListFunctions.register(env);
		LibFunctional.register(env);

		BindPatternTranslator.registerType(domain);
		PatternSymbol.register(coreMap, env);

		MetaObjectSymbols.register(env);

		RegexSymbol.register(env);

		final IfExpressionFactory ifFactory = new IfExpressionFactory(domain);
		ifFactory.registerSymbol(env);

		final LetExpressionFactory letFactory = new LetExpressionFactory(domain, nullValue, colonOperator, assignOperator, lambdaOperator, varArgMarker);
		letFactory.registerSymbol(env);

		final LambdaExpressionFactory lambdaFactory = new LambdaExpressionFactory(nullValue, varArgMarker);
		lambdaFactory.registerSymbol(env);

		final PromiseExpressionFactory delayFactory = new PromiseExpressionFactory(domain);
		delayFactory.registerSymbols(env);

		final MatchExpressionFactory matchFactory = new MatchExpressionFactory(domain, splitOperator, lambdaOperator);
		matchFactory.registerSymbols(envMap, coreMap);

		final AltExpressionFactory altFactory = new AltExpressionFactory(domain, nullValue, colonOperator, assignOperator, splitOperator);
		altFactory.registerSymbol(env);

		final DoExpressionFactory doFactory = new DoExpressionFactory(domain);
		doFactory.registerSymbol(env);

		class TypedValueCompilersFactory extends BasicCompilerMapFactory<TypedValue> {

			@Override
			protected MappedCompilerState<TypedValue> createCompilerState(IAstParser<TypedValue> parser) {
				return new MappedCompilerState<TypedValue>(parser) {
					@Override
					protected IExprNode<TypedValue> createDefaultSymbolNode(String symbol, List<IExprNode<TypedValue>> children) {
						return new VarArgSymbolCallNode(varArgMarker, symbol, children);
					}
				};
			}

			@Override
			protected void configureCompilerStateCommon(MappedCompilerState<TypedValue> compilerState, Environment<TypedValue> environment) {
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_QUOTE, new QuoteStateTransition.ForSymbol(domain, nullValue, valueParser));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_CODE, new CodeStateTransition(domain, compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_IF, ifFactory.createStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_LET, letFactory.createLetStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_LETSEQ, letFactory.createLetSeqStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_LETREC, letFactory.createLetRecStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_DELAY, delayFactory.createStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_MATCH, matchFactory.createStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_AND_THEN, new LazyArgsSymbolTransition(compilerState, domain, TypedCalcConstants.SYMBOL_AND_THEN));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_OR_ELSE, new LazyArgsSymbolTransition(compilerState, domain, TypedCalcConstants.SYMBOL_OR_ELSE));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_NON_NULL, new LazyArgsSymbolTransition(compilerState, domain, TypedCalcConstants.SYMBOL_NON_NULL));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_CONSTANT, new ConstantSymbolStateTransition<TypedValue>(compilerState, environment, TypedCalcConstants.SYMBOL_CONSTANT));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_ALT, altFactory.createStateTransition(compilerState));
				compilerState.addStateTransition(TypedCalcConstants.SYMBOL_DO, doFactory.createStateTransition(compilerState));

				compilerState.addStateTransition(TypedCalcConstants.MODIFIER_QUOTE, new QuoteStateTransition.ForModifier(domain, nullValue, valueParser));
				compilerState.addStateTransition(TypedCalcConstants.MODIFIER_OPERATOR_WRAP, new CallableGetModifierTransition(domain, operators));
				compilerState.addStateTransition(TypedCalcConstants.MODIFIER_INTERPOLATE, new StringInterpolate.StringInterpolateModifier(domain, valuePrinter));
			}

			@Override
			protected DefaultExprNodeFactory<TypedValue> createExprNodeFactory(IValueParser<TypedValue> valueParser) {
				return new MappedExprNodeFactory<TypedValue>(valueParser)
						.addFactory(dotOperator, new IBinaryExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
								return new DotExprNode.Basic(varArgMarker, leftChild, rightChild, dotOperator, domain);
							}
						})
						.addFactory(nullAwareDotOperator, new IBinaryExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
								return new DotExprNode.NullAware(leftChild, rightChild, nullAwareDotOperator, domain);
							}
						})
						.addFactory(lambdaOperator, lambdaFactory.createLambdaExprNodeFactory(lambdaOperator))
						.addFactory(varArgMarker, new MarkerUnaryOperatorNodeFactory(varArgMarker))
						.addFactory(assignOperator, new SingularBinaryOperatorNodeFactory(assignOperator))
						.addFactory(splitOperator, new MarkerBinaryOperatorNodeFactory(splitOperator))
						.addFactory(andOperator, LazyBinaryOperatorNode.createFactory(andOperator, domain, TypedCalcConstants.SYMBOL_AND_THEN))
						.addFactory(orOperator, LazyBinaryOperatorNode.createFactory(orOperator, domain, TypedCalcConstants.SYMBOL_OR_ELSE))
						.addFactory(nonNullOperator, LazyBinaryOperatorNode.createFactory(nonNullOperator, domain, TypedCalcConstants.SYMBOL_NON_NULL))
						.addFactory(defaultOperator, new IBinaryExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
								if (rightChild instanceof SquareBracketContainerNode) {
									// a[...]
									return new SliceCallNode(leftChild, rightChild);
								} else if (rightChild instanceof ArgBracketNode && !isNumericValueNode(leftChild)) {
									if (leftChild instanceof SymbolGetNode) {
										// @a(...)
										final String symbol = ((SymbolGetNode<TypedValue>)leftChild).symbol();
										final List<? extends IExprNode<TypedValue>> args = ImmutableList.copyOf(rightChild.getChildren());
										return new VarArgSymbolCallNode(varArgMarker, symbol, args);
									} else {
										// (a)(...), a(...)(...)
										return new ApplyCallNode(varArgMarker, leftChild, rightChild);
									}
								} else {
									// 5I
									return new BinaryOpNode<TypedValue>(multiplyOperator, leftChild, rightChild);
								}
							}
						})
						.addFactory(nullAwareOperator, new IBinaryExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
								if (rightChild instanceof SquareBracketContainerNode) {
									// a?[...]
									return new SliceCallNode.NullAware(leftChild, rightChild, domain);
								} else if (rightChild instanceof ArgBracketNode) {
									// a?(...)
									return new ApplyCallNode.NullAware(varArgMarker, leftChild, rightChild, domain);
								} else throw new UnsupportedOperationException("Operator '?' cannot be used with " + rightChild);
							}
						})
						.addFactory(SquareBracketContainerNode.BRACKET_OPEN, new IBracketExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(List<IExprNode<TypedValue>> children) {
								return new ListBracketNode(children);
							}
						})
						.addFactory(TypedCalcConstants.BRACKET_ARG_PACK, new IBracketExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(List<IExprNode<TypedValue>> children) {
								return new ArgBracketNode(children);
							}
						})
						.addFactory(TypedCalcConstants.BRACKET_CODE, new IBracketExprNodeFactory<TypedValue>() {
							@Override
							public IExprNode<TypedValue> create(List<IExprNode<TypedValue>> children) {
								Preconditions.checkState(children.size() == 1, "Expected only one expression in curly brackets");
								return new RawCodeExprNode(domain, children.get(0));
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
								})
								.addModifierStateProvider(TypedCalcConstants.MODIFIER_INTERPOLATE, new IStateProvider<TypedValue>() {
									@Override
									public IPostfixCompilerState<TypedValue> createState() {
										return new StringInterpolate.StringInterpolatePostfixCompilerState(domain, valuePrinter);
									}
								});
			}

			@Override
			protected void setupPrefixTokenizer(Tokenizer tokenizer) {
				super.setupPrefixTokenizer(tokenizer);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_QUOTE);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_CDR);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_OPERATOR_WRAP);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_INTERPOLATE);
			}

			@Override
			protected void setupInfixTokenizer(Tokenizer tokenizer) {
				super.setupInfixTokenizer(tokenizer);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_QUOTE);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_CDR);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_OPERATOR_WRAP);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_INTERPOLATE);
			}

			@Override
			protected void setupPostfixTokenizer(Tokenizer tokenizer) {
				super.setupPostfixTokenizer(tokenizer);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_QUOTE);
				tokenizer.addModifier(TypedCalcConstants.MODIFIER_INTERPOLATE);
			}
		}

		final Compilers<TypedValue, ExprType> compilers = new TypedValueCompilersFactory().create(nullValue, valueParser, operators, env);

		// bit far from other definitions, but requires compilers...
		env.setGlobalSymbol("eval", new EvalSymbol(compilers));

		return new Calculator<TypedValue, ExprType>(env, compilers, valuePrinter);
	}
}
