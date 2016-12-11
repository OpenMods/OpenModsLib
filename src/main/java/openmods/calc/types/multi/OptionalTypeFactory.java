package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import openmods.calc.Environment;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.NullaryFunction;
import openmods.calc.UnaryFunction;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class OptionalTypeFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;
	private final TypedValue absentValue;
	private final TypedValue typeValue;
	private final TypedValue presentTypeValue;
	private final TypedValue absentTypeValue;

	private static final String MEMBER_MAP = "map";
	private static final String MEMBER_OR_CALL = "orCall";
	private static final String MEMBER_OR = "or";
	private static final String MEMBER_GET = "get";

	private TypedValue wrap(ICallable<TypedValue> callable) {
		return domain.create(CallableValue.class, new CallableValue(callable));
	}

	private abstract class OptionalValue {

		private final Map<String, TypedValue> members;

		public OptionalValue() {
			final ImmutableMap.Builder<String, TypedValue> members = ImmutableMap.builder();

			members.put(MEMBER_GET, wrap(new NullaryFunction<TypedValue>() {
				@Override
				protected TypedValue call() {
					return OptionalValue.this.getValue();
				}
			}));

			members.put(MEMBER_OR, wrap(new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					return OptionalValue.this.or(value);
				}
			}));

			members.put(MEMBER_OR_CALL, wrap(new FixedCallable<TypedValue>(1, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final TypedValue arg = frame.stack().pop();
					OptionalValue.this.orCall(frame, arg);

				}
			}));

			members.put(MEMBER_MAP, wrap(new FixedCallable<TypedValue>(1, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final TypedValue arg = frame.stack().pop();
					frame.stack().push(OptionalValue.this.map(frame, arg));

				}
			}));

			members.put("isPresent", domain.create(Boolean.class, isPresent()));

			this.members = members.build();
		}

		public Optional<TypedValue> attr(String key) {
			return Optional.fromNullable(members.get(key));
		}

		public abstract TypedValue or(TypedValue value);

		public abstract void orCall(Frame<TypedValue> frame, TypedValue arg);

		public abstract TypedValue map(Frame<TypedValue> frame, TypedValue arg);

		public abstract boolean isPresent();

		public abstract TypedValue getValue();

		public abstract String str();

		public abstract String repr();

	}

	private class Present extends OptionalValue {
		private final TypedValue value;

		public Present(TypedValue value) {
			this.value = value;
		}

		@Override
		public boolean isPresent() {
			return true;
		}

		@Override
		public TypedValue getValue() {
			return value;
		}

		@Override
		public String str() {
			return "present: " + value;
		}

		@Override
		public String repr() {
			return "optional.present(" + value + ")";
		}

		@Override
		public TypedValue or(TypedValue value) {
			return this.value;
		}

		@Override
		public void orCall(Frame<TypedValue> frame, TypedValue arg) {
			Preconditions.checkArgument(MetaObjectUtils.isCallable(arg), "Value is not callable: %s", arg);
			frame.stack().push(value);
		}

		@Override
		public TypedValue map(Frame<TypedValue> frame, TypedValue arg) {
			final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 0);
			final Stack<TypedValue> stack = executionFrame.stack();

			stack.push(Present.this.value);

			MetaObjectUtils.call(executionFrame, arg, OptionalInt.ONE, OptionalInt.ONE);

			final TypedValue result = stack.pop();

			if (!stack.isEmpty())
				throw new IllegalStateException("Values left on stack: " + Lists.newArrayList(stack));

			return present(result);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((value == null)? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;

			if (obj instanceof Present) {
				final Present other = (Present)obj;
				return other.value.equals(this.value);
			}

			return false;
		}

		private OptionalTypeFactory getOuterType() {
			return OptionalTypeFactory.this;
		}

	}

	private class Absent extends OptionalValue {

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public TypedValue getValue() {
			throw new IllegalStateException("No value");
		}

		@Override
		public String str() {
			return "absent";
		}

		@Override
		public String repr() {
			return "optional.absent()";
		}

		@Override
		public TypedValue or(TypedValue value) {
			return value;
		}

		@Override
		public void orCall(Frame<TypedValue> frame, TypedValue arg) {
			MetaObjectUtils.call(frame, arg, OptionalInt.ZERO, OptionalInt.ONE);

		}

		@Override
		public TypedValue map(Frame<TypedValue> frame, TypedValue arg) {
			Preconditions.checkArgument(MetaObjectUtils.isCallable(arg), "Value is not callable: %s", arg);
			return absentValue;
		}

	}

	public OptionalTypeFactory(TypedValue nullValue) {
		this.domain = nullValue.domain;
		this.nullValue = nullValue;
		this.presentTypeValue = createPresentConstructor();
		this.absentTypeValue = createAbsentConstructor();
		this.typeValue = createOptionalType();

		this.domain.registerType(OptionalValue.class, "optional",
				MetaObject.builder()
						.set(new MetaObject.SlotAttr() {
							@Override
							public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
								return self.as(OptionalValue.class).attr(key);
							}
						})
						.set(new MetaObject.SlotStr() {
							@Override
							public String str(TypedValue self, Frame<TypedValue> frame) {
								return self.as(OptionalValue.class).str();
							}
						})
						.set(new MetaObject.SlotRepr() {
							@Override
							public String repr(TypedValue self, Frame<TypedValue> frame) {
								return self.as(OptionalValue.class).repr();
							}
						})
						.set(new MetaObject.SlotBool() {
							@Override
							public boolean bool(TypedValue self, Frame<TypedValue> frame) {
								return self.as(OptionalValue.class).isPresent();
							}

						})
						.set(new MetaObject.SlotType() {
							@Override
							public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
								return typeValue;
							}
						})
						.build());
		this.absentValue = domain.create(OptionalValue.class, new Absent());
	}

	private TypedValue createPresentConstructor() {
		return domain.create(TypeUserdata.class, new TypeUserdata("optional.present"),
				MetaObject.builder()
						.set(MetaObjectUtils.callableAdapter(new UnaryFunction<TypedValue>() {
							@Override
							protected TypedValue call(TypedValue value) {
								return domain.create(OptionalValue.class, new Present(value));
							}
						}))
						.set(new MetaObject.SlotDecompose() {
							@Override
							public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
								Preconditions.checkArgument(variableCount == 1, "Invalid number of values to unpack, expected none got %s", variableCount);
								if (input.is(OptionalValue.class)) {
									final OptionalValue optional = input.as(OptionalValue.class);
									if (!optional.isPresent()) return Optional.absent();

									final List<TypedValue> result = Lists.newArrayList(optional.getValue());
									return Optional.of(result);
								}

								return Optional.absent();
							}

						})
						.set(TypeUserdata.defaultStrSlot)
						.set(TypeUserdata.defaultReprSlot)
						.set(TypeUserdata.defaultAttrSlot(domain))
						.build());
	}

	private static final Optional<List<TypedValue>> ABSENT_MATCH = Optional.of(Collections.<TypedValue> emptyList());

	private TypedValue createAbsentConstructor() {
		return domain.create(TypeUserdata.class, new TypeUserdata("optional.absent"),
				MetaObject.builder()
						.set(MetaObjectUtils.callableAdapter(new NullaryFunction<TypedValue>() {
							@Override
							protected TypedValue call() {
								return absentValue;
							}
						}))
						.set(new MetaObject.SlotDecompose() {
							@Override
							public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
								Preconditions.checkArgument(variableCount == 0, "Invalid number of values to unpack, expected none got %s", variableCount);
								if (input.is(OptionalValue.class)) {
									if (input.as(OptionalValue.class).isPresent()) return Optional.absent();
									else return ABSENT_MATCH;
								}

								return Optional.absent();
							}
						})
						.set(TypeUserdata.defaultStrSlot)
						.set(TypeUserdata.defaultReprSlot)
						.set(TypeUserdata.defaultAttrSlot(domain))
						.build());

	}

	private TypedValue createOptionalType() {
		final Map<String, TypedValue> methods = ImmutableMap.<String, TypedValue> builder()
				.put("from", wrap(new UnaryFunction<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue value) {
						if (value == nullValue)
							return absentValue;
						else
							return domain.create(OptionalValue.class, new Present(value));
					}
				}))
				.put("present", presentTypeValue)
				.put("absent", absentTypeValue)
				.put("name", domain.create(String.class, "optional"))
				.build();

		return domain.create(TypeUserdata.class, new TypeUserdata("optional"),
				MetaObject.builder()
						.set(TypeUserdata.defaultStrSlot)
						.set(TypeUserdata.defaultReprSlot)
						.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
						.set(MetaObjectUtils.attrFromMap(methods))
						.build());
	}

	public TypedValue getAbsent() {
		return absentValue;
	}

	public TypedValue present(TypedValue value) {
		return domain.create(OptionalValue.class, new Present(value));
	}

	public TypedValue wrapNullable(TypedValue result) {
		return result != null? present(result) : absentValue;
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol("optional", typeValue);
	}

}
