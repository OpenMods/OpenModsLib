package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.calc.Frame;
import openmods.calc.ICallable;
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
		public Object getTypeMarker();
	}

	private class StructValue extends SimpleComposite implements CompositeTraits.Structured, CompositeTraits.Printable, StructValueTrait {

		private final Object typeMarker;
		private final Set<String> fields;
		private final Map<String, TypedValue> values;

		private String toStringCache;

		private class UpdateMethod extends SingleReturnCallable<TypedValue> {
			@Override
			public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
				final int argCount = argumentsCount.or(fields.size());

				final Stack<TypedValue> args = frame.stack().substack(argCount);
				final Map<String, TypedValue> newValues = Maps.newHashMap(values);
				extractValues(args, fields, newValues);
				final TypedValue result = domain.create(IComposite.class, new StructValue(typeMarker, fields, newValues));
				args.clear();
				return result;
			}
		}

		public StructValue(Object typeMarker, Set<String> fields, Map<String, TypedValue> values) {
			this.typeMarker = typeMarker;
			this.fields = fields;

			final Map<String, TypedValue> valuesCopy = Maps.newHashMap(values);
			valuesCopy.put("_update", domain.create(ICallable.class, new UpdateMethod()));

			this.values = ImmutableMap.copyOf(valuesCopy);
		}

		@Override
		public String type() {
			return "struct";
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			if (toStringCache == null) {
				final List<String> entries = Lists.newArrayList();
				for (String field : fields)
					entries.add(field + "=" + printer.str(values.get(field)));

				toStringCache = "struct: {" + Joiner.on(",").join(entries) + "}";
			}

			return toStringCache;
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			return Optional.fromNullable(values.get(component));
		}

		@Override
		public Object getTypeMarker() {
			return typeMarker;
		}

	}

	private IComposite createSymbolFactory(List<String> fields) {
		final Object typeMarker = new Object();
		final Set<String> fieldNames = Sets.newLinkedHashSet();
		fieldNames.addAll(fields);

		final List<TypedValue> wrappedFieldNames = Lists.newArrayList();
		for (String fieldName : fields)
			wrappedFieldNames.add(domain.create(String.class, fieldName));

		final TypedValue fieldsList = Cons.createList(wrappedFieldNames, nullValue);

		class CallableConstructor extends SingleReturnCallable<TypedValue> implements CompositeTraits.Callable {
			@Override
			public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
				final int argCount = argumentsCount.or(fieldNames.size());

				final Stack<TypedValue> args = frame.stack().substack(argCount);
				final Map<String, TypedValue> values = Maps.newHashMap();

				for (String field : fieldNames)
					values.put(field, nullValue);

				extractValues(args, fieldNames, values);
				values.put(MEMBER_FIELDS, fieldsList);
				final TypedValue result = domain.create(IComposite.class, new StructValue(typeMarker, fieldNames, values));
				args.clear();

				return result;
			}

		}

		return new MappedComposite.Builder()
				.put(CompositeTraits.Decomposable.class, new CompositeTraits.Decomposable() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(final TypedValue input, int variableCount) {
						Preconditions.checkArgument(variableCount == 1, "Invalid number of values to unpack, expected 1 got %s", variableCount);

						return TypedCalcUtils.tryDecomposeTrait(input, StructValueTrait.class, new Function<StructValueTrait, Optional<List<TypedValue>>>() {
							@Override
							public Optional<List<TypedValue>> apply(StructValueTrait trait) {
								if (trait.getTypeMarker() == typeMarker) {
									final List<TypedValue> result = ImmutableList.of(input);
									return Optional.of(result);
								} else {
									return Optional.absent();
								}
							}
						});
					}

				})
				.put(CompositeTraits.Callable.class, new CallableConstructor())
				.put(CompositeTraits.Structured.class, new CompositeTraits.Structured() {
					@Override
					public Optional<TypedValue> get(TypeDomain domain, String component) {
						if (component.equals(MEMBER_FIELDS)) return Optional.of(fieldsList);
						return Optional.absent();
					}
				})
				.build("structtype");
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

		final TypedValue result = domain.create(IComposite.class, createSymbolFactory(fields));
		args.clear();
		return result;

	}

}
