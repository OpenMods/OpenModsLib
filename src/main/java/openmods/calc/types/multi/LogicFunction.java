package openmods.calc.types.multi;

import com.google.common.base.Optional;
import java.util.Iterator;
import openmods.calc.Frame;
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

		TypedValue arg;
		do {
			arg = it.next();
			if (shouldReturn(arg)) break;
		} while (it.hasNext());

		args.clear();

		return arg;
	}

	protected abstract boolean shouldReturn(TypedValue value);

}
