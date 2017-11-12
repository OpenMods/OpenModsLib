package openmods.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Stack<E> implements Iterable<E> {

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

	public void pushAll(Collection<E> values) {
		data.addAll(values);
	}

	public void checkIsNonEmpty() {
		if (isEmpty()) throw new StackUnderflowException();
	}

	private void checkIndex(int index) {
		if (index < bottomElement) throw new StackUnderflowException();
	}

	public E pop() {
		checkIsNonEmpty();
		try {
			return data.remove(data.size() - 1);
		} catch (IndexOutOfBoundsException e) {
			throw new StackUnderflowException();
		}
	}

	public E popAndExpectEmptyStack() {
		if (size() != 1) throw new StackUnderflowException("Expected exactly one element, got %d, contents: %s", size(), printContents());
		return pop();
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
		checkIsNonEmpty();
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
		return new Stack<>();
	}

	@Override
	public Iterator<E> iterator() {
		return data.listIterator(bottomElement);
	}

	public void clear() {
		if (bottomElement == 0) data.clear();
		final int size = data.size();
		if (size - bottomElement > 0) data.subList(bottomElement, size).clear();
	}

	public Stack<E> substack(int depth) {
		final int newBottom = data.size() - depth;
		if (newBottom < bottomElement) throw new StackUnderflowException(String.format("Not enough elements to create substack: required %s, size %d", depth, size()));
		return newBottom == 0? this : new Stack<>(data, newBottom);
	}

	public Stack<E> checkIsEmpty() {
		if (!isEmpty()) throw new StackValidationException("Expected empty stack, but actually contains: %s", printContents());
		return this;
	}

	public Stack<E> checkSizeIsExactly(int expectedSize) {
		if (size() != expectedSize) throw new StackUnderflowException("Expected stack size %d, got %d, contents: %s", expectedSize, size(), printContents());
		return this;
	}

	public Stack<E> checkSizeIsAtLeast(int expectedSize) {
		if (size() < expectedSize) throw new StackUnderflowException("Expected stack size >= %d, got %d, contents: %s", expectedSize, size(), printContents());
		return this;
	}

	public String printContents() {
		return Iterables.toString(this);
	}

	@Override
	public String toString() {
		return printContents();
	}
}
