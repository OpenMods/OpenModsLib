package openmods.calc;

import openmods.utils.Stack;

public class CompiledFunction<E> extends FixedCallable<E> {

	private final IExecutable<E> body;
	private final Frame<E> scope;

	public CompiledFunction(int argCount, int resultCount, IExecutable<E> body, Frame<E> scope) {
		super(argCount, resultCount);
		this.body = body;
		this.scope = scope;
	}

	@Override
	public void call(Frame<E> frame) {
		final Frame<E> newFrame = FrameFactory.newLocalFrameWithSubstack(scope, argCount);

		final Stack<E> resultStack = newFrame.stack();
		for (int i = 0; i < argCount; i++) {
			E arg = resultStack.pop();
			newFrame.symbols().put("_" + (i + 1), arg);
		}

		body.execute(newFrame);

		resultStack.checkSizeIsExactly(this.resultCount);
	}
}
