package openmods.utils;

import com.google.common.base.Preconditions;

public class Variant {
	// TODO maybe capture type somehow?
	public abstract static class Selector<T> {
		public abstract T cast(Object o);
	}

	public static <T> Selector<T> createSelector() {
		return new Selector<T>() {
			@Override
			@SuppressWarnings("unchecked")
			public T cast(Object o) {
				return (T)o;
			}
		};
	}

	private final Selector<?> type;
	private final Object payload;

	public <T> Variant(Selector<? super T> type, T payload) {
		this.type = type;
		this.payload = payload;
	}

	public boolean is(Selector<?> type) {
		return this.type == type;
	}

	public <T> T get(Selector<T> type) {
		Preconditions.checkArgument(this.type == type);
		return type.cast(payload);
	}
}
