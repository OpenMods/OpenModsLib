package openmods.utils;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Stack<E> {

	public static class StackUnderflowException extends RuntimeException {
		private static final long serialVersionUID = 360455673552034663L;
	}

	private final List<E> data;

	public Stack() {
		this.data = Lists.newArrayList();
	}

	public Stack(int initialCapacity) {
		this.data = Lists.newArrayListWithCapacity(initialCapacity);
	}

	public void push(E value) {
		data.add(value);
	}

	public E pop() {
		Preconditions.checkState(!data.isEmpty());
		try {
			return data.remove(data.size() - 1);
		} catch (IndexOutOfBoundsException e) {
			throw new StackUnderflowException();
		}
	}

	public E peek(int index) {
		return data.get(data.size() - 1 - index);
	}

	public void dup() {
		E last = data.get(data.size() - 1);
		data.add(last);
	}

	public int size() {
		return data.size();
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public static <T> Stack<T> create() {
		return new Stack<T>();
	}
}
