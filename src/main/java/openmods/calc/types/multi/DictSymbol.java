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
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.IValuePrinter;
import openmods.calc.SingleReturnCallable;
import openmods.calc.UnaryFunction;
import openmods.utils.Stack;

public class DictSymbol extends SimpleComposite implements CompositeTraits.Callable, CompositeTraits.TypeMarker {

	private final TypedValue nullValue;

	private final TypeDomain domain;

	private final TypedValue selfValue;

	public DictSymbol(TypedValue nullValue) {
		this.nullValue = nullValue;
		this.domain = nullValue.domain;
		this.selfValue = domain.create(IComposite.class, this);
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

	private class Dict extends SimpleComposite implements CompositeTraits.Structured, CompositeTraits.Indexable, CompositeTraits.Countable, CompositeTraits.Printable, CompositeTraits.Equatable, CompositeTraits.Typed {

		private final Map<TypedValue, TypedValue> values;

		private final Map<String, TypedValue> members = Maps.newHashMap();

		private final Map<String, DelayedValue> delayedMembers = Maps.newHashMap();

		private class UpdateMethod extends SingleReturnCallable<TypedValue> {
			@Override
			public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
				Preconditions.checkState(argumentsCount.isPresent(), "This method requires arguments count");
				final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());

				final Map<TypedValue, TypedValue> newValues = Maps.newHashMap(values);
				extractKeyValuesPairs(args, newValues);
				final TypedValue result = domain.create(IComposite.class, new Dict(newValues));
				args.clear();
				return result;
			}
		}

		private class RemoveMethod extends SingleReturnCallable<TypedValue> {
			@Override
			public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
				Preconditions.checkState(argumentsCount.isPresent(), "This method requires arguments count");
				final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());

				final Map<TypedValue, TypedValue> newValues = Maps.newHashMap(values);
				for (TypedValue arg : args)
					newValues.remove(arg);

				final TypedValue result = domain.create(IComposite.class, new Dict(newValues));
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

			members.put("update", domain.create(ICallable.class, new UpdateMethod()));
			members.put("remove", domain.create(ICallable.class, new RemoveMethod()));
			members.put("hasKey", domain.create(ICallable.class, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue key) {
					return domain.create(Boolean.class, Dict.this.values.containsKey(key));
				}
			}));
			members.put("hasValue", domain.create(ICallable.class, new UnaryFunction<TypedValue>() {
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

		@Override
		public String type() {
			return "map";
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			final List<String> entries = Lists.newArrayList();
			for (Map.Entry<TypedValue, TypedValue> e : values.entrySet())
				entries.add(printer.str(e.getKey()) + ":" + printer.str(e.getValue()));

			return "dict(" + Joiner.on(",").join(entries) + ")";
		}

		@Override
		public int count() {
			return values.size();
		}

		@Override
		public Optional<TypedValue> get(TypedValue index) {
			return Optional.of(Objects.firstNonNull(values.get(index), nullValue));
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			TypedValue member = members.get(component);
			if (member == null && !delayedMembers.isEmpty()) {
				final DelayedValue delayedValue = delayedMembers.remove(component);
				if (delayedValue != null) {
					member = delayedValue.get();
					members.put(component, member);
				}
			}

			return Optional.fromNullable(member);
		}

		@Override
		public boolean isEqual(TypedValue value) {
			if (value.value == this) return true;

			if (value.value instanceof Dict) {
				final Dict other = (Dict)value.value;
				return other.values.equals(this.values);
			}

			return false;
		}

		@Override
		public TypedValue getType() {
			return DictSymbol.this.selfValue;
		}

	}

	@Override
	public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		Preconditions.checkState(argumentsCount.isPresent(), "'dict' symbol requires arguments count");
		final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.get());

		final Map<TypedValue, TypedValue> values = Maps.newHashMap();
		extractKeyValuesPairs(stack, values);

		final TypedValue result = domain.create(IComposite.class, new Dict(values));
		stack.clear();
		stack.push(result);
	}

	@Override
	public String type() {
		return "dict_type";
	}

	public TypedValue value() {
		return selfValue;
	}

}
