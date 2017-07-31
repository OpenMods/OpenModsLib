package openmods.utils;

public abstract class OptionalInt {

	private static final int CACHE_RANGE = 16;

	private static class Present extends OptionalInt {
		private final int value;

		public Present(int value) {
			this.value = value;
		}

		@Override
		public boolean isPresent() {
			return true;
		}

		@Override
		public int get() {
			return value;
		}

		@Override
		public int or(int defaultValue) {
			return value;
		}

		@Override
		public boolean compareIfPresent(int value) {
			return value == this.value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + value;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;

			if (obj instanceof Present) {
				final Present other = (Present)obj;
				return this.value == other.value;
			}

			return false;
		}

		@Override
		public String toString() {
			return "present: " + value;
		}

		@Override
		public OptionalInt map(IntFunction function) {
			return new Present(function.apply(value));
		}

		@Override
		public Integer asNullable() {
			return value;
		}
	}

	private static class Absent extends OptionalInt {

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public int get() {
			throw new IllegalStateException("No value");
		}

		@Override
		public int or(int defaultValue) {
			return defaultValue;
		}

		@Override
		public boolean compareIfPresent(int value) {
			return true;
		}

		@Override
		public String toString() {
			return "absent";
		}

		@Override
		public OptionalInt map(IntFunction function) {
			return this;
		}

		@Override
		public Integer asNullable() {
			return null;
		}

	}

	private OptionalInt() {}

	public abstract boolean isPresent();

	public abstract int get();

	public abstract int or(int defaultValue);

	public abstract boolean compareIfPresent(int value);

	public interface IntFunction {
		public int apply(int value);
	}

	public abstract OptionalInt map(IntFunction function);

	public abstract Integer asNullable();

	private static final OptionalInt[] cache = new OptionalInt[CACHE_RANGE * 2 + 1];

	public static final OptionalInt ABSENT = new Absent();

	static {
		for (int i = 0; i < CACHE_RANGE * 2 + 1; i++)
			cache[i] = new Present(i - CACHE_RANGE);
	}

	public static final OptionalInt ZERO = of(0);
	public static final OptionalInt ONE = of(1);
	public static final OptionalInt TWO = of(2);

	public static OptionalInt of(int value) {
		if (Math.abs(value) <= CACHE_RANGE) return cache[value + CACHE_RANGE];
		return new Present(value);
	}

	public static OptionalInt fromNullable(Integer value) {
		if (value == null) return ABSENT;
		return of(value.intValue());
	}

	public static OptionalInt absent() {
		return ABSENT;
	}
}
