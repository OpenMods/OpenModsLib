package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.SingleReturnCallable;
import openmods.utils.Stack;

public abstract class LogicFunction extends SingleReturnCallable<TypedValue> {

	private final TypedValue nullValue;

	public LogicFunction(TypedValue nullValue) {
		this.nullValue = nullValue;
	}

	@Override
	public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
		final int argCount = argumentsCount.or(2);

		if (argCount == 0)
			return nullValue;

		final Stack<TypedValue> args = frame.stack().substack(argCount);
		final Iterator<TypedValue> it = args.iterator();

		final Frame<TypedValue> scratchFrame = FrameFactory.newLocalFrame(frame);
		final Stack<TypedValue> scratchStack = scratchFrame.stack();

		TypedValue arg;
		do {
			execute(scratchFrame, it.next());
			arg = scratchStack.pop();
			Preconditions.checkState(scratchStack.isEmpty(), "Values left on stack");
			if (shouldReturn(scratchFrame, arg)) break;
		} while (it.hasNext());

		args.clear();

		return arg;
	}

	protected abstract boolean shouldReturn(Frame<TypedValue> scratch, TypedValue arg);

	protected abstract void execute(Frame<TypedValue> scratch, TypedValue value);

	public abstract static class Eager extends LogicFunction {

		public Eager(TypedValue nullValue) {
			super(nullValue);
		}

		@Override
		protected void execute(Frame<TypedValue> scratch, TypedValue value) {
			scratch.stack().push(value);
		}

	}

	public abstract static class Shorting extends LogicFunction {

		public Shorting(TypedValue nullValue) {
			super(nullValue);
		}

		@Override
		protected void execute(Frame<TypedValue> scratch, TypedValue value) {
			final Code code = value.as(Code.class);

			code.execute(scratch);

			final Stack<TypedValue> stack = scratch.stack();
			Preconditions.checkState(stack.size() == 1, "More than one value returned from %s", code);
		}

	}
}
