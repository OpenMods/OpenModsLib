package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class MetaObject {

	public interface Slot {}

	public interface SlotAdapter<T extends Slot> {
		public void call(T slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount);

		public T wrap(TypedValue callable);
	}

	public static interface SlotWithValue extends Slot {
		public TypedValue getValue();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface SlotField {
		public Class<? extends SlotAdapter<? extends Slot>> adapter();
	}

	private MetaObject(Builder builder) {
		this.slotAttr = builder.slotAttr;
		this.slotBool = builder.slotBool;
		this.slotCall = builder.slotCall;
		this.slotEquals = builder.slotEquals;
		this.slotLength = builder.slotLength;
		this.slotRepr = builder.slotRepr;
		this.slotSlice = builder.slotSlice;
		this.slotStr = builder.slotStr;
		this.slotType = builder.slotType;
		this.slotDecompose = builder.slotDecompose;
		this.slotDir = builder.slotDir;
	}

	private static <T extends Slot> T update(T update, T original) {
		return update != null? update : original;
	}

	private MetaObject(MetaObject prev, Builder builder) {
		this.slotAttr = update(builder.slotAttr, prev.slotAttr);
		this.slotBool = update(builder.slotBool, prev.slotBool);
		this.slotCall = update(builder.slotCall, prev.slotCall);
		this.slotEquals = update(builder.slotEquals, prev.slotEquals);
		this.slotLength = update(builder.slotLength, prev.slotLength);
		this.slotRepr = update(builder.slotRepr, prev.slotRepr);
		this.slotSlice = update(builder.slotSlice, prev.slotSlice);
		this.slotStr = update(builder.slotStr, prev.slotStr);
		this.slotType = update(builder.slotType, prev.slotType);
		this.slotDecompose = update(builder.slotDecompose, prev.slotDecompose);
		this.slotDir = update(builder.slotDir, prev.slotDir);
	}

	public interface SlotBool extends Slot {
		public boolean bool(TypedValue self, Frame<TypedValue> frame);
	}

	private static TypedValue callFunction(Frame<TypedValue> frame, TypedValue callable, TypedValue self, TypedValue arg) {
		final Stack<TypedValue> substack = frame.stack().substack(0);
		substack.push(self);
		substack.push(arg);
		final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(frame, substack);
		MetaObjectUtils.call(executionFrame, callable, OptionalInt.TWO, OptionalInt.ONE);
		return substack.popAndExpectEmptyStack();
	}

	private static TypedValue callFunction(Frame<TypedValue> frame, TypedValue callable, TypedValue self) {
		final Stack<TypedValue> substack = frame.stack().substack(0);
		substack.push(self);
		final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(frame, substack);
		MetaObjectUtils.call(executionFrame, callable, OptionalInt.ONE, OptionalInt.ONE);
		return substack.popAndExpectEmptyStack();
	}

	public static class SlotBoolAdapter implements SlotAdapter<SlotBool> {

		@Override
		public void call(SlotBool slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue self = stack.pop();
			final boolean result = slot.bool(self, frame);
			stack.push(self.domain.create(Boolean.class, result));
		}

		@Override
		public SlotBool wrap(final TypedValue callable) {
			class WrappedSlot implements SlotBool, SlotWithValue {
				@Override
				public boolean bool(TypedValue self, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self).as(Boolean.class);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotBoolAdapter.class)
	public final SlotBool slotBool;

	public interface SlotLength extends Slot {
		public int length(TypedValue self, Frame<TypedValue> frame);
	}

	public static class SlotLengthAdapter implements SlotAdapter<SlotLength> {

		@Override
		public void call(SlotLength slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue self = stack.pop();
			final int result = slot.length(self, frame);
			stack.push(self.domain.create(BigInteger.class, BigInteger.valueOf(result)));
		}

		@Override
		public SlotLength wrap(final TypedValue callable) {
			class WrappedSlot implements SlotLength, SlotWithValue {
				@Override
				public int length(TypedValue self, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self).as(BigInteger.class).intValue();
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotLengthAdapter.class)
	public final SlotLength slotLength;

	public interface SlotAttr extends Slot {
		public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame);
	}

	public static class SlotAttrAdapter implements SlotAdapter<SlotAttr> {

		@Override
		public void call(SlotAttr slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(2);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();

			final String key = stack.pop().as(String.class);
			final TypedValue self = stack.pop();
			final Optional<TypedValue> result = slot.attr(self, key, frame);
			stack.push(OptionalType.fromOptional(self.domain, result));
		}

		@Override
		public SlotAttr wrap(final TypedValue callable) {
			class WrappedSlot implements SlotAttr, SlotWithValue {
				@Override
				public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
					final Stack<TypedValue> substack = frame.stack().substack(0);
					substack.push(self);
					substack.push(self.domain.create(String.class, key));
					final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(frame, substack);
					MetaObjectUtils.call(executionFrame, callable, OptionalInt.TWO, OptionalInt.ONE);
					final TypedValue result = substack.pop();
					return result.as(OptionalType.Value.class).asOptional();
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotAttrAdapter.class)
	public final SlotAttr slotAttr;

	public interface SlotEquals extends Slot {
		public boolean equals(TypedValue self, TypedValue value, Frame<TypedValue> frame);
	}

	public static class SlotEqualsAdapter implements SlotAdapter<SlotEquals> {

		@Override
		public void call(SlotEquals slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(2);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();

			final TypedValue other = stack.pop();
			final TypedValue self = stack.pop();

			final boolean result = slot.equals(self, other, frame);
			stack.push(self.domain.create(Boolean.class, result));
		}

		@Override
		public SlotEquals wrap(final TypedValue callable) {
			class WrappedSlot implements SlotEquals, SlotWithValue {
				@Override
				public boolean equals(TypedValue self, TypedValue value, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self, value).as(Boolean.class);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotEqualsAdapter.class)
	public final SlotEquals slotEquals;

	public interface SlotCall extends Slot {
		public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame);
	}

	private static final OptionalInt.IntFunction ADD_SELF_TO_COUNT = new OptionalInt.IntFunction() {
		@Override
		public int apply(int value) {
			return value + 1;
		}
	};

	public static class SlotCallAdapter implements SlotAdapter<SlotCall> {

		@Override
		public void call(SlotCall slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			final int argCount = argumentsCount.get() - 1;
			final TypedValue self = frame.stack().drop(argCount);
			slot.call(self, OptionalInt.of(argCount), returnsCount, frame);
		}

		@Override
		public SlotCall wrap(final TypedValue callable) {
			class WrappedSlot implements SlotCall, SlotWithValue {
				@Override
				public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
					final Stack<TypedValue> substack = frame.stack().substack(argumentsCount.get());
					final List<TypedValue> args = ImmutableList.copyOf(substack);
					substack.clear();
					substack.push(self);
					substack.pushAll(args);

					final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(frame, substack);
					MetaObjectUtils.call(executionFrame, callable, argumentsCount.map(ADD_SELF_TO_COUNT), returnsCount);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotCallAdapter.class)
	public final SlotCall slotCall;

	public interface SlotType extends Slot {
		public TypedValue type(TypedValue self, Frame<TypedValue> frame);
	}

	public static class SlotTypeAdapter implements SlotAdapter<SlotType> {

		@Override
		public void call(SlotType slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue self = stack.pop();
			final TypedValue result = slot.type(self, frame);
			stack.push(result);
		}

		@Override
		public SlotType wrap(final TypedValue callable) {
			class WrappedSlot implements SlotType, SlotWithValue {
				@Override
				public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotTypeAdapter.class)
	public final SlotType slotType;

	public interface SlotSlice extends Slot {
		public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame);
	}

	public static class SlotSliceAdapter implements SlotAdapter<SlotSlice> {

		@Override
		public void call(SlotSlice slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(2);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();

			final TypedValue range = stack.pop();
			final TypedValue self = stack.pop();
			final TypedValue result = slot.slice(self, range, frame);
			stack.push(result);
		}

		@Override
		public SlotSlice wrap(final TypedValue callable) {
			class WrappedSlot implements SlotSlice, SlotWithValue {
				@Override
				public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self, range);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotSliceAdapter.class)
	public final SlotSlice slotSlice;

	public interface SlotStr extends Slot {
		public String str(TypedValue self, Frame<TypedValue> frame);
	}

	public static class SlotStrAdapter implements SlotAdapter<SlotStr> {

		@Override
		public void call(SlotStr slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue self = stack.pop();
			final String result = slot.str(self, frame);
			stack.push(self.domain.create(String.class, result));
		}

		@Override
		public SlotStr wrap(final TypedValue callable) {
			class WrappedSlot implements SlotStr, SlotWithValue {
				@Override
				public String str(TypedValue self, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self).as(String.class);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}

	}

	@SlotField(adapter = SlotStrAdapter.class)
	public final SlotStr slotStr;

	public interface SlotRepr extends Slot {
		public String repr(TypedValue self, Frame<TypedValue> frame);
	}

	public static class SlotReprAdapter implements SlotAdapter<SlotRepr> {

		@Override
		public void call(SlotRepr slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue self = stack.pop();
			final String result = slot.repr(self, frame);
			stack.push(self.domain.create(String.class, result));

		}

		@Override
		public SlotRepr wrap(final TypedValue callable) {
			class WrappedSlot implements SlotRepr, SlotWithValue {
				@Override
				public String repr(TypedValue self, Frame<TypedValue> frame) {
					return callFunction(frame, callable, self).as(String.class);
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}

	}

	@SlotField(adapter = SlotReprAdapter.class)
	public final SlotRepr slotRepr;

	public interface SlotDecompose extends Slot {
		public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame);
	}

	public static class SlotDecomposeAdapter implements SlotAdapter<SlotDecompose> {

		@Override
		public void call(SlotDecompose slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final int count = stack.pop().as(BigInteger.class).intValue();
			final TypedValue input = stack.pop();
			final TypedValue self = stack.pop();
			final Optional<List<TypedValue>> result = slot.tryDecompose(self, input, count, frame);
			final TypeDomain domain = self.domain;
			if (result.isPresent()) {
				final Iterator<TypedValue> it = result.get().iterator();
				TypedValue resultList = it.next();
				while (it.hasNext()) {
					final TypedValue nextResult = it.next();
					resultList = domain.create(Cons.class, new Cons(nextResult, resultList));
				}

				stack.push(OptionalType.present(self.domain, resultList));
			} else {
				stack.push(OptionalType.absent(self.domain));
			}
		}

		@Override
		public SlotDecompose wrap(final TypedValue callable) {
			class WrappedSlot implements SlotDecompose, SlotWithValue {
				@Override
				public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
					final Stack<TypedValue> stack = frame.stack().substack(0);

					stack.push(self);
					stack.push(input);
					stack.push(self.domain.create(BigInteger.class, BigInteger.valueOf(variableCount)));

					final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(frame, stack);
					MetaObjectUtils.call(executionFrame, callable, OptionalInt.of(3), OptionalInt.of(1));

					final TypedValue result = stack.popAndExpectEmptyStack();

					final OptionalType.Value maybeResult = result.as(OptionalType.Value.class);
					if (maybeResult.isPresent()) {
						TypedValue currentResult = maybeResult.getValue();

						final List<TypedValue> unpackedResults = Lists.newArrayList();

						while (true) {
							if (currentResult.is(Cons.class)) {
								final Cons pair = currentResult.as(Cons.class);
								unpackedResults.add(pair.car);
								currentResult = pair.cdr;
							} else {
								unpackedResults.add(currentResult);
								break;
							}
						}
						return Optional.of(unpackedResults);
					} else {
						return Optional.absent();
					}
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}

			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotDecomposeAdapter.class)
	public final SlotDecompose slotDecompose;

	public interface SlotDir extends Slot {
		public Iterable<String> dir(TypedValue self, Frame<TypedValue> frame);
	}

	public static class SlotDirAdapter implements SlotAdapter<SlotDir> {

		@Override
		public void call(SlotDir slot, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			argumentsCount.compareIfPresent(1);
			returnsCount.compareIfPresent(1);
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue self = stack.pop();
			final Iterable<String> result = slot.dir(self, frame);
			final TypeDomain domain = self.domain;
			final List<TypedValue> wrappedResults = ImmutableList.copyOf(Iterables.transform(result, domain.createWrappingTransformer(String.class)));
			final TypedValue nullValue = domain.getDefault(UnitType.class);
			stack.push(Cons.createList(wrappedResults, nullValue));
		}

		@Override
		public SlotDir wrap(final TypedValue callable) {
			class WrappedSlot implements SlotDir, SlotWithValue {
				@Override
				public Iterable<String> dir(TypedValue self, Frame<TypedValue> frame) {
					final TypedValue result = callFunction(frame, callable, self);
					final TypeDomain domain = self.domain;
					return Iterables.transform(Cons.toIterable(result, domain.getDefault(UnitType.class)), domain.createUnwrappingTransformer(String.class));
				}

				@Override
				public TypedValue getValue() {
					return callable;
				}
			}
			return new WrappedSlot();
		}
	}

	@SlotField(adapter = SlotDirAdapter.class)
	public final SlotDir slotDir;

	public static class Builder {
		private SlotBool slotBool;

		private SlotLength slotLength;

		private SlotAttr slotAttr;

		private SlotEquals slotEquals;

		private SlotCall slotCall;

		private SlotType slotType;

		private SlotSlice slotSlice;

		private SlotStr slotStr;

		private SlotRepr slotRepr;

		private SlotDecompose slotDecompose;

		private SlotDir slotDir;

		public Builder set(SlotBool slotBool) {
			Preconditions.checkState(this.slotBool == null);
			this.slotBool = slotBool;
			return this;
		}

		public Builder set(SlotLength slotLength) {
			Preconditions.checkState(this.slotLength == null);
			this.slotLength = slotLength;
			return this;
		}

		public Builder set(SlotAttr slotAttr) {
			Preconditions.checkState(this.slotAttr == null);
			this.slotAttr = slotAttr;
			return this;
		}

		public Builder set(SlotEquals slotEquals) {
			Preconditions.checkState(this.slotEquals == null);
			this.slotEquals = slotEquals;
			return this;
		}

		public Builder set(SlotCall slotCall) {
			Preconditions.checkState(this.slotCall == null);
			this.slotCall = slotCall;
			return this;
		}

		public Builder set(SlotType slotType) {
			Preconditions.checkState(this.slotType == null);
			this.slotType = slotType;
			return this;
		}

		public Builder set(SlotSlice slotSlice) {
			Preconditions.checkState(this.slotSlice == null);
			this.slotSlice = slotSlice;
			return this;
		}

		public Builder set(SlotStr slotStr) {
			Preconditions.checkState(this.slotStr == null);
			this.slotStr = slotStr;
			return this;
		}

		public Builder set(SlotRepr slotRepr) {
			Preconditions.checkState(this.slotRepr == null);
			this.slotRepr = slotRepr;
			return this;
		}

		public Builder set(SlotDecompose slotDecompose) {
			Preconditions.checkState(this.slotDecompose == null);
			this.slotDecompose = slotDecompose;
			return this;
		}

		public Builder set(SlotDir slotDir) {
			Preconditions.checkState(this.slotDir == null);
			this.slotDir = slotDir;
			return this;
		}

		public MetaObject build() {
			return new MetaObject(this);
		}

		public MetaObject update(MetaObject meta) {
			return new MetaObject(meta, this);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

}
