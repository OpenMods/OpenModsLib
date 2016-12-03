package openmods.calc.types.multi;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.calc.Frame;
import openmods.calc.IValuePrinter;
import openmods.calc.SingleReturnCallable;
import openmods.utils.Stack;

public class StructSymbol extends SingleReturnCallable<TypedValue> {

	private static final String MEMBER_FIELDS = "_fields";

	private final TypedValue nullValue;
	private final TypeDomain domain;

	public StructSymbol(TypedValue nullValue) {
		this.nullValue = nullValue;
		this.domain = nullValue.domain;
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

	private interface StructValueTrait extends ICompositeTrait {
		public StructType getTypeMarker();
	}

	private class StructValue extends SimpleComposite implements CompositeTraits.Structured, CompositeTraits.Printable, CompositeTraits.Equatable, CompositeTraits.Typed, CompositeTraits.Callable, StructValueTrait {

		private final StructType type;
		private final TypedValue typeValue;

		private final Set<String> fields;
		private final Map<String, TypedValue> values;

		public StructValue(StructType type, TypedValue typeValue, Set<String> fields, Map<String, TypedValue> values) {
			this.type = type;
			this.typeValue = typeValue;
			this.fields = fields;
			this.values = ImmutableMap.copyOf(values);
		}

		@Override
		public String type() {
			return "struct";
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			final List<String> entries = Lists.newArrayList();
			for (String field : fields)
				entries.add(field + "=" + printer.str(values.get(field)));

			return "struct: {" + Joiner.on(",").join(entries) + "}";
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			return Optional.fromNullable(values.get(component));
		}

		@Override
		public StructType getTypeMarker() {
			return type;
		}

		@Override
		public boolean isEqual(TypedValue value) {
			if (value.value == this) return true;

			if (value.value instanceof StructValue) {
				final StructValue other = (StructValue)value.value;
				return other.type == this.type
						&& other.values.equals(this.values);
			}

			return false;
		}

		@Override
		public TypedValue getType() {
			return typeValue;
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			TypedCalcUtils.expectSingleReturn(returnsCount);

			final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.or(fields.size()));
			final Map<String, TypedValue> newValues = Maps.newHashMap(values);
			extractValues(stack, fields, newValues);
			stack.clear();
			stack.push(domain.create(IComposite.class, new StructValue(type, typeValue, fields, newValues)));
		}

	}

	private class StructType extends SimpleComposite implements CompositeTraits.Callable, CompositeTraits.Structured, CompositeTraits.TypeMarker {

		private final Set<String> fieldNames = Sets.newLinkedHashSet();
		private final List<TypedValue> wrappedFieldNames = Lists.newArrayList();
		private final TypedValue fieldsList;
		private final TypedValue selfValue;

		public StructType(List<String> fields) {
			this.fieldNames.addAll(fields);

			for (String fieldName : fields)
				wrappedFieldNames.add(domain.create(String.class, fieldName));

			fieldsList = Cons.createList(wrappedFieldNames, nullValue);

			selfValue = domain.create(IComposite.class, this);
		}

		@Override
		public String type() {
			return "struct_type";
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			if (component.equals(MEMBER_FIELDS)) return Optional.of(fieldsList);
			return Optional.absent();
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			TypedCalcUtils.expectSingleReturn(returnsCount);

			final int argCount = argumentsCount.or(fieldNames.size());

			final Stack<TypedValue> stack = frame.stack().substack(argCount);
			final Map<String, TypedValue> values = Maps.newHashMap();

			for (String field : fieldNames)
				values.put(field, nullValue);

			extractValues(stack, fieldNames, values);
			values.put(MEMBER_FIELDS, fieldsList);
			final TypedValue result = domain.create(IComposite.class, new StructValue(this, selfValue, fieldNames, values));
			stack.clear();
			stack.push(result);
		}

	}

	@Override
	public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
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

		final TypedValue result = domain.create(IComposite.class, new StructType(fields));
		args.clear();
		return result;

	}

}
