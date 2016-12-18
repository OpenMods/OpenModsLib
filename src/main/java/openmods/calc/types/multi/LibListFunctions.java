package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import openmods.calc.BinaryFunction;
import openmods.calc.Environment;
import openmods.calc.Frame;
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
	}

}
