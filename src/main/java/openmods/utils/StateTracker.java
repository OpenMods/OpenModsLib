package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import openmods.Log;

public class StateTracker<T extends Enum<T>> {

	public static class StateUpdater<T extends Enum<T>> {
		private final String name;
		private T state;

		public StateUpdater(String name, T state) {
			this.name = name;
			this.state = state;
		}

		public T state() {
			return state;
		}

		public void update(T state) {
			Log.trace("State of %s updated from %s to %s", name, this.state, state);
			this.state = state;
		}

		public String name() {
			return name;
		}

		@Override
		public String toString() {
			return "[" + name + ":" + state + "]";
		}
	}

	private final Map<String, StateUpdater<T>> states = Maps.newHashMap();

	private final T defaultInitialState;

	public StateTracker(T defaultInitialState) {
		this.defaultInitialState = defaultInitialState;
	}

	public StateUpdater<T> register(String name) {
		return register(name, defaultInitialState);
	}

	public StateUpdater<T> register(String name, T initialState) {
		StateUpdater<T> state = new StateUpdater<>(name, initialState);
		StateUpdater<T> prev = states.put(name, state);
		Preconditions.checkState(prev == null, "Duplicated tracked name: %s", name);
		return state;
	}

	public Collection<StateUpdater<T>> states() {
		return Collections.unmodifiableCollection(states.values());
	}

	public static <T extends Enum<T>> StateTracker<T> create(T initialState) {
		return new StateTracker<>(initialState);
	}
}
