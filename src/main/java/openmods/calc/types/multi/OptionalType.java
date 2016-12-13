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

public class OptionalType {

	private static final String MEMBER_MAP = "map";
	private static final String MEMBER_OR_CALL = "orCall";
	private static final String MEMBER_OR = "or";
	private static final String MEMBER_GET = "get";

	private static TypedValue wrap(TypeDomain domain, ICallable<TypedValue> callable) {
		return domain.create(CallableValue.class, new CallableValue(callable));
	}

	public static abstract class Value {

		private final Map<String, TypedValue> members;

		protected TypeDomain domain;

		public Value(TypeDomain domain) {
			this.domain = domain;
			final ImmutableMap.Builder<String, TypedValue> members = ImmutableMap.builder();

			members.put(MEMBER_GET, wrap(domain, new NullaryFunction<TypedValue>() {
				@Override
				protected TypedValue call() {
					return Value.this.getValue();
				}
			}));

			members.put(MEMBER_OR, wrap(domain, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					return Value.this.or(value);
				}
			}));

			members.put(MEMBER_OR_CALL, wrap(domain, new FixedCallable<TypedValue>(1, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final TypedValue arg = frame.stack().pop();
					Value.this.orCall(frame, arg);

				}
			}));

			members.put(MEMBER_MAP, wrap(domain, new FixedCallable<TypedValue>(1, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final TypedValue arg = frame.stack().pop();
					frame.stack().push(Value.this.map(frame, arg));

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

		public abstract Optional<TypedValue> asOptional();
	}

	private static class Present extends Value {
		private final TypedValue value;

		public Present(TypeDomain domain, TypedValue value) {
			super(domain);
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

			return present(domain, result);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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

		@Override
		public Optional<TypedValue> asOptional() {
			return Optional.of(value);
		}
	}

	private static class Absent extends Value {

		public Absent(TypeDomain domain) {
			super(domain);
		}

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
			return domain.create(Value.class, this);
		}

		@Override
		public Optional<TypedValue> asOptional() {
			return Optional.absent();
		}

		@Override
		public int hashCode() {
			return 42;
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof Absent;
		}

	}

	private static TypedValue createPresentConstructor(final TypeDomain domain) {
		return domain.create(TypeUserdata.class, new TypeUserdata("optional.present", Value.class),
				MetaObject.builder()
						.set(MetaObjectUtils.callableAdapter(new UnaryFunction<TypedValue>() {
							@Override
							protected TypedValue call(TypedValue value) {
								return domain.create(Value.class, new Present(domain, value));
							}
						}))
						.set(new MetaObject.SlotDecompose() {
							@Override
							public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
								Preconditions.checkArgument(variableCount == 1, "Invalid number of values to unpack, expected none got %s", variableCount);
								if (input.is(Value.class)) {
									final Value optional = input.as(Value.class);
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

	private static TypedValue createAbsentConstructor(final TypeDomain domain) {
		return domain.create(TypeUserdata.class, new TypeUserdata("optional.absent", Value.class),
				MetaObject.builder()
						.set(MetaObjectUtils.callableAdapter(new NullaryFunction<TypedValue>() {
							@Override
							protected TypedValue call() {
								return absent(domain);
							}
						}))
						.set(new MetaObject.SlotDecompose() {
							@Override
							public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
								Preconditions.checkArgument(variableCount == 0, "Invalid number of values to unpack, expected none got %s", variableCount);
								if (input.is(Value.class)) {
									if (input.as(Value.class).isPresent()) return Optional.absent();
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

	private static TypedValue createOptionalType(final TypedValue nullValue, final TypedValue presentTypeValue, final TypedValue absentTypeValue) {
		final TypeDomain domain = nullValue.domain;
		final Map<String, TypedValue> methods = ImmutableMap.<String, TypedValue> builder()
				.put("from", wrap(domain, new UnaryFunction<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue value) {
						return (value == nullValue)? absent(domain) : present(domain, value);
					}
				}))
				.put("present", presentTypeValue)
				.put("absent", absentTypeValue)
				.put("name", domain.create(String.class, "optional"))
				.build();

		return domain.create(TypeUserdata.class, new TypeUserdata("optional", Value.class),
				MetaObject.builder()
						.set(TypeUserdata.defaultStrSlot)
						.set(TypeUserdata.defaultReprSlot)
						.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
						.set(MetaObjectUtils.attrFromMap(methods))
						.build());
	}

	public static void register(Environment<TypedValue> env, TypedValue nullValue) {
		final TypeDomain domain = nullValue.domain;
		final TypedValue presentConstructor = createPresentConstructor(domain);
		final TypedValue absentConstructor = createAbsentConstructor(domain);

		final TypedValue typeValue = createOptionalType(nullValue, presentConstructor, absentConstructor);

		env.setGlobalSymbol("optional", typeValue);

		domain.registerType(Value.class, "optional",
				MetaObject.builder()
						.set(new MetaObject.SlotAttr() {
							@Override
							public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
								return self.as(Value.class).attr(key);
							}
						})
						.set(new MetaObject.SlotStr() {
							@Override
							public String str(TypedValue self, Frame<TypedValue> frame) {
								return self.as(Value.class).str();
							}
						})
						.set(new MetaObject.SlotRepr() {
							@Override
							public String repr(TypedValue self, Frame<TypedValue> frame) {
								return self.as(Value.class).repr();
							}
						})
						.set(new MetaObject.SlotBool() {
							@Override
							public boolean bool(TypedValue self, Frame<TypedValue> frame) {
								return self.as(Value.class).isPresent();
							}

						})
						.set(new MetaObject.SlotType() {
							@Override
							public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
								return typeValue;
							}
						})
						.build());
	}

	public static TypedValue absent(TypeDomain domain) {
		return domain.create(Value.class, new Absent(domain));
	}

	public static TypedValue present(TypeDomain domain, TypedValue value) {
		return domain.create(Value.class, new Present(domain, value));
	}

	public static TypedValue wrapNullable(TypeDomain domain, TypedValue result) {
		return result != null? present(domain, result) : absent(domain);
	}

	public static TypedValue fromOptional(TypeDomain domain, Optional<TypedValue> value) {
		return value.isPresent()? present(domain, value.get()) : absent(domain);
	}
}
