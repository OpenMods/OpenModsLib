package openmods.calc.types.multi;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import openmods.calc.BinaryFunction;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.SingleReturnCallable;
import openmods.calc.UnaryFunction;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class DictSymbol {

	private final TypedValue nullValue;

	private final TypeDomain domain;

	private final TypedValue selfValue;

	private final OptionalTypeFactory optionalFactory;

	public DictSymbol(TypedValue nullValue, OptionalTypeFactory optionalFactory) {
		this.nullValue = nullValue;
		this.domain = nullValue.domain;
		this.domain.registerType(Dict.class, "dict", createValueMetaObject());
		this.optionalFactory = optionalFactory;
		this.selfValue = domain.create(TypeUserdata.class, new TypeUserdata("dict"), createTypeMetaObject());
	}

	private MetaObject createTypeMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						Preconditions.checkState(argumentsCount.isPresent(), "'dict' symbol requires arguments count");
						TypedCalcUtils.expectSingleReturn(returnsCount);
						final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.get());

						final Map<TypedValue, TypedValue> values = Maps.newHashMap();
						extractKeyValuesPairs(stack, values);

						final TypedValue result = domain.create(Dict.class, new Dict(values));
						stack.clear();
						stack.push(result);

					}
				})
				.set(TypeUserdata.typeReprSlot)
				.set(TypeUserdata.typeStrSlot)
				.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
				.build();
	}

	private static void extractKeyValuesPairs(Iterable<TypedValue> args, Map<TypedValue, TypedValue> output) {
		for (TypedValue arg : args) {
			if (arg.is(Cons.class)) {
				final Cons pair = arg.as(Cons.class);
				output.put(pair.car, pair.cdr);
			} else {
				throw new IllegalArgumentException("Expected key(symbol):value pair, got " + arg);
			}
		}
	}

	private interface DelayedValue {
		public TypedValue get();
	}

	private class Dict {
		private final Map<TypedValue, TypedValue> values;

		private final Map<String, TypedValue> members = Maps.newHashMap();

		private final Map<String, DelayedValue> delayedMembers = Maps.newHashMap();

		private class UpdateMethod extends SingleReturnCallable<TypedValue> {
			@Override
			public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
				Preconditions.checkState(argumentsCount.isPresent(), "This method requires arguments count");
				final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());

				final Map<TypedValue, TypedValue> newValues = Maps.newHashMap(values);
				extractKeyValuesPairs(args, newValues);
				final TypedValue result = domain.create(Dict.class, new Dict(newValues));
				args.clear();
				return result;
			}
		}

		private class RemoveMethod extends SingleReturnCallable<TypedValue> {
			@Override
			public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
				Preconditions.checkState(argumentsCount.isPresent(), "This method requires arguments count");
				final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());

				final Map<TypedValue, TypedValue> newValues = Maps.newHashMap(values);
				for (TypedValue arg : args)
					newValues.remove(arg);

				final TypedValue result = domain.create(Dict.class, new Dict(newValues));
				args.clear();
				return result;
			}
		}

		private abstract class EntriesAccessor implements DelayedValue {
			@Override
			public TypedValue get() {
				// final list will be reversed, but it's still hash map, so order is undetermined anyway
				TypedValue result = nullValue;

				for (Map.Entry<TypedValue, TypedValue> e : values.entrySet()) {
					final TypedValue element = getEntry(e);
					result = domain.create(Cons.class, new Cons(element, result));
				}

				return result;
			}

			protected abstract TypedValue getEntry(Map.Entry<TypedValue, TypedValue> e);
		}

		public Dict(Map<TypedValue, TypedValue> values) {
			this.values = ImmutableMap.copyOf(values);

			members.put("update", CallableValue.wrap(domain, new UpdateMethod()));
			members.put("remove", CallableValue.wrap(domain, new RemoveMethod()));
			members.put("hasKey", CallableValue.wrap(domain, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue key) {
					return domain.create(Boolean.class, Dict.this.values.containsKey(key));
				}
			}));
			members.put("getOptional", CallableValue.wrap(domain, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue key) {
					return optionalFactory.wrapNullable(Dict.this.values.get(key));
				}
			}));
			members.put("getOr", CallableValue.wrap(domain, new BinaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue key, TypedValue defaultValue) {
					final TypedValue result = Dict.this.values.get(key);
					return result != null? result : defaultValue;
				}
			}));
			members.put("getOrCall", CallableValue.wrap(domain, new FixedCallable<TypedValue>(2, 1) {
				@Override
				public void call(Frame<TypedValue> frame) {
					final Stack<TypedValue> stack = frame.stack();
					final TypedValue defaultFunction = stack.pop();
					final TypedValue key = stack.pop();
					final TypedValue result = Dict.this.values.get(key);
					if (result != null) {
						stack.push(result);
					} else {
						MetaObjectUtils.call(frame, defaultFunction, OptionalInt.ZERO, OptionalInt.ONE);
					}

				}
			}));
			members.put("hasValue", CallableValue.wrap(domain, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					return domain.create(Boolean.class, Dict.this.values.containsValue(value));
				}
			}));

			delayedMembers.put("keys", new EntriesAccessor() {
				@Override
				protected TypedValue getEntry(Entry<TypedValue, TypedValue> e) {
					return e.getKey();
				}
			});
			delayedMembers.put("values", new EntriesAccessor() {
				@Override
				protected TypedValue getEntry(Entry<TypedValue, TypedValue> e) {
					return e.getValue();
				}
			});
			delayedMembers.put("items", new EntriesAccessor() {
				@Override
				protected TypedValue getEntry(Entry<TypedValue, TypedValue> e) {
					return domain.create(Cons.class, new Cons(e.getKey(), e.getValue()));
				}
			});
		}

		public Optional<TypedValue> attr(String key) {
			TypedValue member = members.get(key);
			if (member == null && !delayedMembers.isEmpty()) {
				final DelayedValue delayedValue = delayedMembers.remove(key);
				if (delayedValue != null) {
					member = delayedValue.get();
					members.put(key, member);
				}
			}

			return Optional.fromNullable(member);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((values == null)? 0 : values.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) return true;

			if (o instanceof Dict) {
				final Dict other = (Dict)o;
				return other.values.equals(this.values);
			}

			return false;
		}
	}

	private MetaObject createValueMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						TypedCalcUtils.expectSingleReturn(returnsCount);
						Preconditions.checkState(argumentsCount.isPresent(), "This method requires arguments count");

						final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.get());

						final Dict dict = self.as(Dict.class);
						final Map<TypedValue, TypedValue> newValues = Maps.newHashMap(dict.values);
						extractKeyValuesPairs(stack, newValues);
						stack.clear();
						stack.push(domain.create(Dict.class, new Dict(newValues)));
					}
				})
				.set(new MetaObject.SlotType() {
					@Override
					public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
						return DictSymbol.this.selfValue;
					}
				})
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						final Dict dict = self.as(Dict.class);
						final List<String> entries = Lists.newArrayList();
						for (Map.Entry<TypedValue, TypedValue> e : dict.values.entrySet())
							entries.add(MetaObjectUtils.callStrSlot(frame, e.getKey()) + ":" + MetaObjectUtils.callStrSlot(frame, e.getValue()));

						return "{" + Joiner.on(",").join(entries) + "}";
					}
				})
				.set(new MetaObject.SlotRepr() {

					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						final Dict dict = self.as(Dict.class);
						final List<String> entries = Lists.newArrayList();
						for (Map.Entry<TypedValue, TypedValue> e : dict.values.entrySet())
							entries.add(MetaObjectUtils.callReprSlot(frame, e.getKey()) + ":" + MetaObjectUtils.callReprSlot(frame, e.getValue()));

						return "dict(" + Joiner.on(",").join(entries) + ")";
					}
				})
				.set(new MetaObject.SlotLength() {
					@Override
					public int length(TypedValue self, Frame<TypedValue> frame) {
						return self.as(Dict.class).values.size();
					}
				})
				.set(new MetaObject.SlotBool() {
					@Override
					public boolean bool(TypedValue self, Frame<TypedValue> frame) {
						return !self.as(Dict.class).values.isEmpty();
					}
				})
				.set(new MetaObject.SlotSlice() {

					@Override
					public TypedValue slice(TypedValue self, TypedValue index, Frame<TypedValue> frame) {
						return Objects.firstNonNull(self.as(Dict.class).values.get(index), nullValue);
					}
				})
				.set(new MetaObject.SlotAttr() {

					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						return self.as(Dict.class).attr(key);
					}
				})
				.set(MetaObjectUtils.USE_VALUE_EQUALS)
				.build();
	}

	public TypedValue value() {
		return selfValue;
	}

}
