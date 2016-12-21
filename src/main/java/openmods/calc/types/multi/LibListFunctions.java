package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import openmods.calc.BinaryFunction;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.SingleReturnCallable;
import openmods.calc.TernaryFunction;
import openmods.calc.UnaryFunction;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.RawReturn;
import openmods.calc.types.multi.TypedFunction.Variant;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class LibListFunctions {

	private static MetaObject.SlotCall getCallableSlot(final TypedValue functor) {
		final MetaObject.SlotCall slotCall = functor.getMetaObject().slotCall;
		Preconditions.checkState(slotCall != null, "Value %s is not callable", functor);
		return slotCall;
	}

	private static TypedValue executeUnaryCallable(Frame<TypedValue> frame, TypedValue callable, MetaObject.SlotCall slotCall, TypedValue arg) {
		final Stack<TypedValue> stack = frame.stack();
		stack.push(arg);
		slotCall.call(callable, OptionalInt.ONE, OptionalInt.ONE, frame);
		return stack.pop();
	}

	private interface KeyFunction {
		public TypedValue apply(TypedValue value);
	}

	private static final KeyFunction NULL_KEY_FUNCTION = new KeyFunction() {
		@Override
		public TypedValue apply(TypedValue value) {
			return value;
		}
	};

	private static class CompositeComparator implements Comparator<TypedValue> {

		private final KeyFunction keyFunction;

		private final Comparator<TypedValue> compareFunction;

		public CompositeComparator(KeyFunction keyFunction, Comparator<TypedValue> compareFunction) {
			this.keyFunction = keyFunction;
			this.compareFunction = compareFunction;
		}

		@Override
		public int compare(TypedValue o1, TypedValue o2) {
			final TypedValue c1 = keyFunction.apply(o1);
			final TypedValue c2 = keyFunction.apply(o2);
			return compareFunction.compare(c1, c2);
		}

	}

	public static void register(Environment<TypedValue> env) {
		final TypedValue nullValue = env.nullValue();
		final TypeDomain domain = nullValue.domain;

		env.setGlobalSymbol("map", new BinaryFunction.WithFrame<TypedValue>() {
			@Override
			protected TypedValue call(final Frame<TypedValue> frame, final TypedValue functor, final TypedValue list) {
				final MetaObject.SlotCall slotCall = getCallableSlot(functor);

				final Stack<TypedValue> stack = frame.stack();
				return new Cons.RecursiveVisitor(nullValue) {
					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						final TypedValue result = executeUnaryCallable(frame, functor, slotCall, head);
						Preconditions.checkState(stack.isEmpty(), "Values left on stack");
						return Cons.create(domain, result, process(tail));
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("filter", new BinaryFunction.WithFrame<TypedValue>() {
			@Override
			protected TypedValue call(final Frame<TypedValue> frame, final TypedValue predicate, final TypedValue list) {
				final MetaObject.SlotCall slotCall = getCallableSlot(predicate);

				final Stack<TypedValue> stack = frame.stack();
				return new Cons.RecursiveVisitor(nullValue) {
					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						final TypedValue result = executeUnaryCallable(frame, predicate, slotCall, head);
						boolean shouldKeep = MetaObjectUtils.boolValue(frame, result);
						Preconditions.checkState(stack.isEmpty(), "Values left on stack");
						return shouldKeep? Cons.create(domain, head, process(tail)) : process(tail);
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("reduce", new TernaryFunction.WithFrame<TypedValue>() {
			@Override
			protected TypedValue call(final Frame<TypedValue> frame, final TypedValue functor, final TypedValue initialValue, final TypedValue list) {
				final MetaObject.SlotCall slotCall = functor.getMetaObject().slotCall;
				final Stack<TypedValue> stack = frame.stack();

				return new Cons.RecursiveVisitor(nullValue) {
					private TypedValue result = initialValue;

					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						stack.push(result);
						stack.push(head);
						slotCall.call(functor, OptionalInt.TWO, OptionalInt.ONE, frame);
						result = stack.pop();
						return process(tail);
					}

					@Override
					protected TypedValue processEnd() {
						return result;
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("take", new BinaryFunction.WithFrame<TypedValue>() {
			@Override
			public TypedValue call(Frame<TypedValue> frame, TypedValue list, TypedValue countArg) {
				final int count = countArg.as(BigInteger.class).intValue();
				Preconditions.checkState(count >= 0, "Invalid count: %s", count);

				return new Cons.RecursiveVisitor(nullValue) {
					private int countdown = count;

					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						if (countdown-- == 0) return nullValue;
						return Cons.create(domain, head, process(tail));
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("takeWhile", new BinaryFunction.WithFrame<TypedValue>() {
			@Override
			public TypedValue call(final Frame<TypedValue> frame, final TypedValue list, final TypedValue predicate) {
				final MetaObject.SlotCall slotCall = getCallableSlot(predicate);
				final Stack<TypedValue> stack = frame.stack();

				return new Cons.RecursiveVisitor(nullValue) {
					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						final TypedValue result = executeUnaryCallable(frame, predicate, slotCall, head);
						boolean shouldContinue = MetaObjectUtils.boolValue(frame, result);
						Preconditions.checkState(stack.isEmpty(), "Values left on stack");
						return shouldContinue? Cons.create(domain, head, process(tail)) : nullValue;
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("drop", new BinaryFunction.WithFrame<TypedValue>() {
			@Override
			public TypedValue call(Frame<TypedValue> frame, TypedValue list, TypedValue countArg) {
				final int count = countArg.as(BigInteger.class).intValue();

				if (count == 0)
					return list;

				Preconditions.checkState(count > 0, "Invalid count: %s", count);

				return new Cons.RecursiveVisitor(nullValue) {
					private int countdown = count;

					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						if (--countdown <= 0) return tail;
						return process(tail);
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("dropWhile", new BinaryFunction.WithFrame<TypedValue>() {
			@Override
			public TypedValue call(final Frame<TypedValue> frame, final TypedValue list, final TypedValue predicate) {
				final MetaObject.SlotCall slotCall = getCallableSlot(predicate);
				final Stack<TypedValue> stack = frame.stack();

				return new Cons.RecursiveVisitor(nullValue) {
					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						final TypedValue result = executeUnaryCallable(frame, predicate, slotCall, head);
						boolean shouldDrop = MetaObjectUtils.boolValue(frame, result);
						Preconditions.checkState(stack.isEmpty(), "Values left on stack");
						return shouldDrop? process(tail) : Cons.create(domain, head, tail);
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("any", new UnaryFunction.WithFrame<TypedValue>() {
			@Override
			public TypedValue call(final Frame<TypedValue> frame, final TypedValue list) {
				boolean result = new Cons.TypedRecursiveVisitor<Boolean>(nullValue) {
					@Override
					protected Boolean processValue(TypedValue head, TypedValue tail) {
						boolean value = MetaObjectUtils.boolValue(frame, head);
						return value || process(tail);
					}

					@Override
					protected Boolean processEnd() {
						return false;
					}
				}.process(list);

				return domain.create(Boolean.class, result);
			}
		});

		env.setGlobalSymbol("all", new UnaryFunction.WithFrame<TypedValue>() {
			@Override
			public TypedValue call(final Frame<TypedValue> frame, final TypedValue list) {
				boolean result = new Cons.TypedRecursiveVisitor<Boolean>(nullValue) {
					@Override
					protected Boolean processValue(TypedValue head, TypedValue tail) {
						boolean value = MetaObjectUtils.boolValue(frame, head);
						return value && process(tail);
					}

					@Override
					protected Boolean processEnd() {
						return true;
					}
				}.process(list);

				return domain.create(Boolean.class, result);
			}
		});

		env.setGlobalSymbol("enumerate", new UnaryFunction.WithFrame<TypedValue>() {
			@Override
			protected TypedValue call(Frame<TypedValue> frame, TypedValue list) {
				return new Cons.RecursiveVisitor(nullValue) {
					int count = 0;

					@Override
					protected TypedValue processValue(TypedValue head, TypedValue tail) {
						final TypedValue wrappedCount = domain.create(BigInteger.class, BigInteger.valueOf(count++));
						final TypedValue enumeratedPair = Cons.create(domain, wrappedCount, head);
						return Cons.create(domain, enumeratedPair, process(tail));
					}
				}.process(list);
			}
		});

		env.setGlobalSymbol("range", new SimpleTypedFunction(domain) {

			@Variant
			@RawReturn
			public TypedValue range(BigInteger stop) {
				return range(0, stop.intValue(), 1);
			}

			@Variant
			@RawReturn
			public TypedValue range(BigInteger start, @DispatchArg BigInteger stop) {
				return range(start.intValue(), stop.intValue(), 1);
			}

			@Variant
			@RawReturn
			public TypedValue range(BigInteger start, BigInteger stop, @DispatchArg BigInteger step) {
				return range(start.intValue(), stop.intValue(), step.intValue());
			}

			private TypedValue range(int start, int stop, int step) {
				Preconditions.checkState(step != 0, "Step cannot be 0");
				final List<TypedValue> result = Lists.newArrayList();

				if (stop >= start) {
					if (step < 0) return nullValue;

					for (int i = start; i < stop; i += step)
						result.add(domain.create(BigInteger.class, BigInteger.valueOf(i)));
				} else {
					if (step > 0) return nullValue;

					for (int i = start; i > stop; i += step)
						result.add(domain.create(BigInteger.class, BigInteger.valueOf(i)));
				}

				return Cons.createList(result, nullValue);
			}
		});

		env.setGlobalSymbol("zip", new BinaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue left, TypedValue right) {
				final Iterator<TypedValue> leftIt = Cons.toIterable(left, nullValue).iterator();
				final Iterator<TypedValue> rightIt = Cons.toIterable(right, nullValue).iterator();

				final List<TypedValue> result = Lists.newArrayList();
				while (leftIt.hasNext() && rightIt.hasNext()) {
					result.add(Cons.create(domain, leftIt.next(), rightIt.next()));
				}

				return Cons.createList(result, nullValue);
			}
		});

		env.setGlobalSymbol("flatten", new SingleReturnCallable<TypedValue>() {
			@Override
			public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
				Preconditions.checkArgument(argumentsCount.isPresent(), "'flatten' required argument count");
				final Stack<TypedValue> values = frame.stack().substack(argumentsCount.get());

				final List<TypedValue> result = Lists.newArrayList();

				for (TypedValue value : values)
					Iterables.addAll(result, Cons.toIterable(value, nullValue));

				values.clear();
				return Cons.createList(result, nullValue);
			}
		});

		env.setGlobalSymbol("sort", new SingleReturnCallable<TypedValue>() {
			@Override
			public TypedValue call(final Frame<TypedValue> frame, OptionalInt argumentsCount) {
				final int args = argumentsCount.or(1);

				Preconditions.checkState(args >= 1, "'sort' expects at least one argument");

				final Stack<TypedValue> stack = frame.stack().substack(args);

				final Map<String, TypedValue> kwdArgs = Maps.newHashMap();
				for (int i = 0; i < args - 1; i++) {
					final TypedValue arg = stack.pop();
					final Cons keyValuePair = arg.as(Cons.class, "optional arg");
					kwdArgs.put(keyValuePair.car.as(Symbol.class, "optional arg name").value, keyValuePair.cdr);
				}

				final TypedValue list = stack.pop();

				List<TypedValue> elements = Lists.newArrayList(Cons.toIterable(list, nullValue));

				final TypedValue keyFunctionArg = kwdArgs.get("key");
				final KeyFunction keyFunction = extractKeyFunction(frame, stack, keyFunctionArg);

				final TypedValue compareFunctionArg = kwdArgs.get("cmp");
				final Comparator<TypedValue> compareFunction = extractCompareFunction(frame, stack, compareFunctionArg);

				Collections.sort(elements, new CompositeComparator(keyFunction, compareFunction));

				final TypedValue reverse = kwdArgs.get("reverse");
				if (reverse != null && MetaObjectUtils.boolValue(frame, reverse))
					elements = Lists.reverse(elements);

				return Cons.createList(elements, nullValue);
			}

			private Comparator<TypedValue> extractCompareFunction(final Frame<TypedValue> frame, final Stack<TypedValue> stack, final TypedValue compareFunctionArg) {
				if (compareFunctionArg != null) {
					final MetaObject.SlotCall slotCall = compareFunctionArg.getMetaObject().slotCall;
					Preconditions.checkState(slotCall != null, "'Compare function' argument %s is not callable", compareFunctionArg);
					return new Comparator<TypedValue>() {
						@Override
						public int compare(TypedValue o1, TypedValue o2) {
							stack.push(o1);
							stack.push(o2);
							slotCall.call(compareFunctionArg, OptionalInt.TWO, OptionalInt.ONE, frame);
							final TypedValue result = stack.pop();
							Preconditions.checkState(stack.isEmpty(), "Values left on stack");
							return result.unwrap(BigInteger.class).intValue();
						}
					};

				} else {
					return new TypedValueComparator();
				}
			}

			private KeyFunction extractKeyFunction(final Frame<TypedValue> frame, final Stack<TypedValue> stack, final TypedValue keyFunctionArg) {
				if (keyFunctionArg != null) {
					final MetaObject.SlotCall slotCall = keyFunctionArg.getMetaObject().slotCall;
					Preconditions.checkState(slotCall != null, "'Key function' argument %s is not callable", keyFunctionArg);
					return new KeyFunction() {
						@Override
						public TypedValue apply(TypedValue value) {
							stack.push(value);
							slotCall.call(keyFunctionArg, OptionalInt.ONE, OptionalInt.ONE, frame);
							final TypedValue result = stack.pop();
							Preconditions.checkState(stack.isEmpty(), "Values left on stack");
							return result;
						}
					};
				} else {
					return NULL_KEY_FUNCTION;
				}
			}
		});
	}

}
