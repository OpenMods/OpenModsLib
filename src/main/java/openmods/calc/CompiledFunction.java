package openmods.calc;

import openmods.utils.Stack;

public class CompiledFunction<E> extends FixedFunctionSymbol<E> {

	private final IExecutable<E> body;
	private final ICalculatorFrame<E> scope;

	public CompiledFunction(int argCount, int resultCount, IExecutable<E> body, ICalculatorFrame<E> scope) {
		super(argCount, resultCount);
		this.body = body;
		this.scope = scope;
	}

	@Override
	public void call(ICalculatorFrame<E> frame) {
		final SubFrame<E> newFrame = new SubFrame<E>(scope, argCount);

		final Stack<E> resultStack = newFrame.stack();
		for (int i = 1; i <= argCount; i++) {
			E arg = resultStack.pop();
			newFrame.setLocalSymbol("$" + i, Constant.create(arg));
		}

		body.execute(newFrame);

		final int left = resultStack.size();
		if (left != 0) throw new StackValidationException("Stack not empty after execution (expected %s, left %s)", this.resultCount, left);
	}
}
