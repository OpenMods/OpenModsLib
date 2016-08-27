package openmods.utils;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;

public class Stack<E> implements Iterable<E> {

	public static class StackUnderflowException extends RuntimeException {
		private static final long serialVersionUID = 360455673552034663L;

		public StackUnderflowException(String message) {
			super(message);
		}

		public StackUnderflowException() {
			super("stack underflow");
		}
	}

	private final List<E> data;
	private final int bottomElement;

	public Stack() {
		this.data = Lists.newArrayList();
		this.bottomElement = 0;
	}

	public Stack(int initialCapacity) {
		this.data = Lists.newArrayListWithCapacity(initialCapacity);
		this.bottomElement = 0;
	}

	private Stack(List<E> data, int bottomElement) {
		this.data = data;
		this.bottomElement = bottomElement;
	}

	public void push(E value) {
		data.add(value);
	}

	private void checkNonEmpty() {
		if (isEmpty()) throw new StackUnderflowException();
	}

	private void checkIndex(int index) {
		if (index < bottomElement) throw new StackUnderflowException();
	}

	public E pop() {
		checkNonEmpty();
		try {
			return data.remove(data.size() - 1);
		} catch (IndexOutOfBoundsException e) {
			throw new StackUnderflowException();
		}
	}

	private int indexFromTop(int index) {
		return data.size() - 1 - index;
	}

	public E peek(int index) {
		final int peekIndex = indexFromTop(index);
		checkIndex(peekIndex);
		return data.get(peekIndex);
	}

	public void dup() {
		checkNonEmpty();
		E last = data.get(data.size() - 1);
		data.add(last);
	}

	public E drop(int index) {
		final int dropIndex = indexFromTop(index);
		checkIndex(dropIndex);
		return data.remove(dropIndex);
	}

	public int size() {
		return data.size() - bottomElement;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public static <T> Stack<T> create() {
		return new Stack<T>();
	}

	@Override
	public Iterator<E> iterator() {
		return data.listIterator(bottomElement);
	}

	public Stack<E> substack(int depth) {
		final int newBottom = data.size() - depth;
		if (newBottom < bottomElement) throw new StackUnderflowException(String.format("Not enough elements to create substack: required %s, size %d", depth, size()));
		return newBottom == 0? this : new Stack<E>(data, newBottom);
	}
}
