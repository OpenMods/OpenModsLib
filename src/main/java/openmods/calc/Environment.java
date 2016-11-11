package openmods.calc;

import openmods.utils.Stack;

public class Environment<E> {

	public static final String VAR_ANS = "_ans";

	private final Frame<E> topFrame = createTopMap();

	private final E nullValue;

	public Environment(E nullValue) {
		this.nullValue = nullValue;
	}

	protected Frame<E> createTopMap() {
		return FrameFactory.createTopFrame();
	}

	public E nullValue() {
		return nullValue;
	}

	public void setGlobalSymbol(String name, ISymbol<E> symbol) {
		topFrame.symbols().put(name, symbol);
	}

	public void setGlobalSymbol(String name, ICallable<E> callable) {
		topFrame.symbols().put(name, callable);
	}

	public void setGlobalSymbol(String name, IGettable<E> gettable) {
		topFrame.symbols().put(name, gettable);
	}

	public void setGlobalSymbol(String name, E value) {
		topFrame.symbols().put(name, value);
	}

	public Frame<E> topFrame() {
		return topFrame;
	}

	public Frame<E> executeIsolated(IExecutable<E> executable) {
		final Frame<E> freshFrame = FrameFactory.newLocalFrame(topFrame);
		executable.execute(freshFrame);
		return freshFrame;
	}

	public void execute(IExecutable<E> executable) {
		executable.execute(topFrame);
	}

	public E executeAndPop(IExecutable<E> executable) {
		executable.execute(topFrame);
		final Stack<E> stack = topFrame.stack();

		if (stack.isEmpty()) {
			topFrame.symbols().put(VAR_ANS, nullValue);
			return null;
		} else {
			final E result = stack.pop();
			topFrame.symbols().put(VAR_ANS, result);
			return result;
		}
	}
}
