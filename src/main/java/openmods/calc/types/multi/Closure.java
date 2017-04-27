package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.symbol.ICallable;
import openmods.calc.symbol.SymbolMap;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class Closure implements ICallable<TypedValue> {

	private final Code code;

	private final SymbolMap<TypedValue> scopeSymbols;

	private final List<IBindPattern> args;

	public Closure(SymbolMap<TypedValue> scopeSymbols, Code code, List<IBindPattern> args) {
		this.code = code;
		this.scopeSymbols = scopeSymbols;
		this.args = ImmutableList.copyOf(args);
	}

	@Override
	public void call(Frame<TypedValue> callsite, OptionalInt argumentsCount, OptionalInt returnsCount) {
		TypedCalcUtils.expectExactArgCount(argumentsCount, args.size());

		final Frame<TypedValue> executionFrame = FrameFactory.newClosureFrame(scopeSymbols, callsite, args.size());
		final Stack<TypedValue> executionStack = executionFrame.stack();

		final SymbolMap<TypedValue> executionSymbols = executionFrame.symbols();

		final Iterator<TypedValue> argValues = executionStack.iterator();
		for (IBindPattern argPattern : args) {
			final TypedValue argValue = argValues.next();
			TypedCalcUtils.matchPattern(argPattern, executionFrame, executionSymbols, argValue);
		}
		executionStack.clear();

		code.execute(executionFrame);

		TypedCalcUtils.expectExactReturnCount(returnsCount, executionStack.size());
	}

}
