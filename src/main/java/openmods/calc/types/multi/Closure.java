package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolMap;
import openmods.utils.Stack;

public class Closure implements ICallable<TypedValue> {

	private final Code code;

	private final SymbolMap<TypedValue> scopeSymbols;

	private final List<String> args;

	public Closure(SymbolMap<TypedValue> scopeSymbols, Code code, List<String> args) {
		this.code = code;
		this.scopeSymbols = scopeSymbols;
		this.args = ImmutableList.copyOf(args).reverse(); // reverse, since we are pulling from stack
	}

	@Override
	public void call(Frame<TypedValue> callsite, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		if (argumentsCount.isPresent()) {
			final int argCount = argumentsCount.get();
			if (argCount != args.size()) throw new StackValidationException("Expected %s argument(s) but got %s", args.size(), argCount);
		}

		final Frame<TypedValue> executionFrame = FrameFactory.newClosureFrame(scopeSymbols, callsite, args.size());
		final Stack<TypedValue> executionStack = executionFrame.stack();

		for (String arg : args)
			executionFrame.symbols().put(arg, executionStack.pop());

		code.execute(executionFrame);

		if (returnsCount.isPresent()) {
			final int expectedReturns = returnsCount.get();
			final int actualReturns = executionStack.size();
			if (expectedReturns != actualReturns) throw new StackValidationException("Has %s result(s) but expected %s", actualReturns, expectedReturns);
		}
	}

}
