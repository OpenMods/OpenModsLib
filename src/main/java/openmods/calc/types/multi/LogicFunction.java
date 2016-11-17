package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.SingleReturnCallable;
import openmods.calc.SymbolMap;
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

		final SymbolMap<TypedValue> symbols = frame.symbols();
		final Stack<TypedValue> args = frame.stack().substack(argCount);
		final Iterator<TypedValue> it = args.iterator();

		TypedValue arg;
		do {
			arg = extract(symbols, it.next());
			if (shouldReturn(arg)) break;
		} while (it.hasNext());

		args.clear();

		return arg;
	}

	protected abstract boolean shouldReturn(TypedValue value);

	protected abstract TypedValue extract(SymbolMap<TypedValue> symbolMap, TypedValue value);

	public abstract static class Eager extends LogicFunction {

		public Eager(TypedValue nullValue) {
			super(nullValue);
		}

		@Override
		protected TypedValue extract(SymbolMap<TypedValue> symbolMap, TypedValue value) {
			return value;
		}

	}

	public abstract static class Shorting extends LogicFunction {

		public Shorting(TypedValue nullValue) {
			super(nullValue);
		}

		@Override
		protected TypedValue extract(SymbolMap<TypedValue> symbolMap, TypedValue value) {
			final Code code = value.as(Code.class);
			final Frame<TypedValue> evalFrame = FrameFactory.symbolsToFrame(symbolMap);

			code.execute(evalFrame);

			final Stack<TypedValue> stack = evalFrame.stack();
			Preconditions.checkState(stack.size() == 1, "More than one value returned from %s", code);

			return stack.pop();
		}

	}
}
