package openmods.calc.types.multi;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import java.util.Locale;
import openmods.calc.Compilers;
import openmods.calc.ExprType;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.StackValidationException;
import openmods.utils.Stack;

public class EvalSymbol implements ICallable<TypedValue> {

	private final Compilers<TypedValue, ExprType> compilers;

	public EvalSymbol(Compilers<TypedValue, ExprType> compilers) {
		this.compilers = compilers;
	}

	@Override
	public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		if (argumentsCount.isPresent()) {
			final int args = argumentsCount.get();
			if (args != 2) throw new StackValidationException("Expected 2 arguments but got %s", args);
		}

		final Stack<TypedValue> stack = frame.stack();
		final String code = stack.pop().as(String.class, "second 'eval' argument");
		final String type = stack.pop().as(String.class, "first 'eval' argument");

		final ExprType exprType;
		try {
			exprType = ExprType.valueOf(type.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(String.format("Value '%s' is not valid expression type, expected one of %s", type, Joiner.on(',').join(ExprType.values())));
		}

		final IExecutable<TypedValue> compiled = compilers.compile(exprType, code);

		final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 0);
		compiled.execute(executionFrame);

		if (returnsCount.isPresent()) {
			final int expected = returnsCount.get();
			final int actual = executionFrame.stack().size();
			if (expected != actual) throw new StackValidationException("Has %s result(s) but expected %s", actual, expected);
		}
	}

}
