package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.SymbolMap;
import openmods.utils.OptionalInt;
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
	public void call(Frame<TypedValue> callsite, OptionalInt argumentsCount, OptionalInt returnsCount) {
		TypedCalcUtils.expectExactArgCount(argumentsCount, args.size());

		final Frame<TypedValue> executionFrame = FrameFactory.newClosureFrame(scopeSymbols, callsite, args.size());
		final Stack<TypedValue> executionStack = executionFrame.stack();

		for (String arg : args)
			executionFrame.symbols().put(arg, executionStack.pop());

		code.execute(executionFrame);

		TypedCalcUtils.expectExactReturnCount(returnsCount, executionStack.size());
	}

}
