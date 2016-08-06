package openmods.calc;

import com.google.common.collect.Iterables;
import openmods.utils.Stack;

public class Environment<E> {

	public static final String VAR_ANS = "$ans";

	private final TopFrame<E> topFrame = new TopFrame<E>();

	private final E nullValue;

	public Environment(E nullValue) {
		this.nullValue = nullValue;
	}

	public E nullValue() {
		return nullValue;
	}

	public void setGlobalSymbol(String id, ISymbol<E> value) {
		topFrame.setSymbol(id, value);
	}

	public int stackSize() {
		return topFrame.stack().size();
	}

	public Iterable<E> getStack() {
		return Iterables.unmodifiableIterable(topFrame.stack());
	}

	public TopFrame<E> executeIsolated(IExecutable<E> executable) {
		final TopFrame<E> freshTopFrame = topFrame.cloneWithSymbols();
		executable.execute(freshTopFrame);
		return freshTopFrame;
	}

	public void execute(IExecutable<E> executable) {
		executable.execute(topFrame);
	}

	public E executeAndPop(IExecutable<E> executable) {
		executable.execute(topFrame);
		final Stack<E> stack = topFrame.stack();

		if (stack.isEmpty()) {
			topFrame.setSymbol(VAR_ANS, Constant.create(nullValue));
			return null;
		} else {
			final E result = stack.pop();
			topFrame.setSymbol(VAR_ANS, Constant.create(result));
			return result;
		}
	}
}
