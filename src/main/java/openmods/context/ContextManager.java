package openmods.context;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class ContextManager {

	private static class Frame {

		private final Frame prev;

		public Frame(Frame prev) {
			this.prev = prev;
		}

		private final Map<VariableKey<?>, Object> values = Maps.newHashMap();

		@SuppressWarnings("unchecked")
		private <T> T get(VariableKey<T> key) {
			Object o = values.get(key);
			if (o != null) return (T)o;

			if (prev != null) return prev.get(key);

			return null;
		}

		@SuppressWarnings("unchecked")
		private <T> T remove(VariableKey<T> key) {
			Object o = values.remove(key);
			if (o != null) return (T)o;

			if (prev != null) return prev.remove(key);
			return null;
		}

		private <T> void put(VariableKey<T> key, T value) {
			values.put(key, value);
		}
	}

	private static final ThreadLocal<Frame> top = new ThreadLocal<Frame>() {
		@Override
		protected Frame initialValue() {
			return new Frame(null);
		}
	};

	public static void push() {
		Frame prevTop = top.get();
		Frame newTop = new Frame(prevTop);
		top.set(newTop);
	}

	public static void pop() {
		Frame currentTop = top.get();
		Preconditions.checkState(currentTop != null, "Trying to pop, but no context available");

		final Frame newTop = currentTop.prev;
		Preconditions.checkState(newTop != null, "Trying to pop last frame");
		top.set(newTop);
	}

	public static <T> T get(VariableKey<T> key) {
		Frame currentTop = top.get();
		Preconditions.checkState(currentTop != null, "No context on stack");
		return currentTop.get(key);
	}

	public static <T> T remove(VariableKey<T> key) {
		Frame currentTop = top.get();
		Preconditions.checkState(currentTop != null, "No context on stack");
		return currentTop.remove(key);
	}

	public static <T> void set(VariableKey<T> key, T value) {
		Frame currentTop = top.get();
		Preconditions.checkState(currentTop != null, "No context on stack");
		currentTop.put(key, value);
	}

}
