package openmods.calc.types.multi;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.calc.Frame;
import openmods.calc.SingleReturnCallable;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class StructSymbol extends SingleReturnCallable<TypedValue> {

	private static final String ATTR_FIELDS = "fields";

	private final TypedValue nullValue;
	private final TypeDomain domain;

	public StructSymbol(TypedValue nullValue) {
		this.nullValue = nullValue;
		this.domain = nullValue.domain;

		this.domain.registerType(StructType.class, "struct_type", createStructTypeMetaObject());
		this.domain.registerType(StructValue.class, "struct", createStructValueMetaObject());
	}

	private static void extractValues(Iterable<TypedValue> args, Set<String> fields, Map<String, TypedValue> output) {
		for (TypedValue arg : args) {
			if (arg.is(Cons.class)) {
				final Cons pair = arg.as(Cons.class);
				Preconditions.checkState(pair.car.is(Symbol.class), "Expected key(symbol):value pair, got %s", arg);
				final String key = pair.car.as(Symbol.class).value;
				Preconditions.checkArgument(fields.contains(key), "Unknown key: %s", key);
				output.put(key, pair.cdr);
			} else {
				throw new IllegalArgumentException("Expected key:value pair, got " + arg);
			}
		}
	}

	private class StructValue {

		private final StructType type;
		private final Map<String, TypedValue> values;

		public StructValue(StructType type, Map<String, TypedValue> values) {
			this.type = type;
			this.values = ImmutableMap.copyOf(values);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((values == null)? 0 : values.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;

			if (obj instanceof StructValue) {
				final StructValue other = (StructValue)obj;
				return (this.type == other.type) &&
						(values.equals(other.values));
			}
			return false;
		}

	}

	private MetaObject createStructValueMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotAttr() {

					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						return Optional.fromNullable(self.as(StructValue.class).values.get(key));
					}
				})
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						final StructValue value = self.as(StructValue.class);
						final List<String> entries = Lists.newArrayList();
						for (String field : value.type.fieldNames)
							entries.add(field + "=" + MetaObjectUtils.callStrSlot(frame, value.values.get(field)));

						return "{" + Joiner.on(",").join(entries) + "}";
					}
				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						final StructValue value = self.as(StructValue.class);
						final List<String> entries = Lists.newArrayList();
						for (String field : value.type.fieldNames)
							entries.add("#" + field + ":" + MetaObjectUtils.callReprSlot(frame, value.values.get(field)));
						return "struct(" + Joiner.on(",").join(entries) + ")";
					}
				})
				.set(new MetaObject.SlotType() {
					@Override
					public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
						return self.as(StructValue.class).type.selfValue;
					}
				})
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						final StructValue value = self.as(StructValue.class);
						TypedCalcUtils.expectSingleReturn(returnsCount);

						final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.or(value.type.fieldNames.size()));
						final Map<String, TypedValue> newValues = Maps.newHashMap(value.values);
						extractValues(stack, value.type.fieldNames, newValues);
						stack.clear();
						stack.push(domain.create(StructValue.class, new StructValue(value.type, newValues)));
					}
				})
				.build();
	}

	private class StructType extends TypeUserdata {

		private final Set<String> fieldNames = Sets.newLinkedHashSet();
		private final TypedValue fieldsList;
		private final TypedValue selfValue;

		public StructType(List<String> fields) {
			super("struct");
			this.fieldNames.addAll(fields);

			final List<TypedValue> wrappedFieldNames = Lists.newArrayList(Iterables.transform(fieldNames, domain.createTransformer(String.class)));
			fieldsList = Cons.createList(wrappedFieldNames, nullValue);
			selfValue = domain.create(StructType.class, this);
		}

	}

	private MetaObject createStructTypeMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						final StructType type = self.as(StructType.class);
						if (key.equals(ATTR_FIELDS)) return Optional.of(type.fieldsList);
						else if (key.equals(TypeUserdata.ATTR_TYPE_NAME)) return Optional.of(domain.create(String.class, "struct"));
						return Optional.absent();
					}
				})
				.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						TypedCalcUtils.expectSingleReturn(returnsCount);
						final StructType type = self.as(StructType.class);
						final int argCount = argumentsCount.or(type.fieldNames.size());

						final Stack<TypedValue> stack = frame.stack().substack(argCount);
						final Map<String, TypedValue> values = Maps.newHashMap();

						for (String field : type.fieldNames)
							values.put(field, nullValue);

						extractValues(stack, type.fieldNames, values);
						final TypedValue result = domain.create(StructValue.class, new StructValue(type, values));
						stack.clear();
						stack.push(result);

					}
				})
				.build();
	}

	@Override
	public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
		Preconditions.checkState(argumentsCount.isPresent(), "'struct' symbol requires arguments count");
		final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());

		final List<String> fields = Lists.newArrayList();

		for (TypedValue arg : args) {
			if (arg.is(Symbol.class)) {
				final Symbol symbol = arg.as(Symbol.class);
				fields.add(symbol.value);
			} else {
				throw new IllegalArgumentException("Expected symbol, got " + arg);
			}
		}

		final TypedValue result = domain.create(StructType.class, new StructType(fields));
		args.clear();
		return result;

	}

}
