package openmods.access;

public abstract class ApiInstanceProvider<T> {
	public static class CachedInstance<T> extends ApiInstanceProvider<T> {
		private final T instance;

		public CachedInstance(Class<? extends T> cls) {
			try {
				instance = cls.newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public T getInterface() {
			return instance;
		}

		@Override
		public String toString() {
			return "SingleInstanceProvider [instance=" + instance + "]";
		}

	}

	public static class NewInstance<T> extends ApiInstanceProvider<T> {
		private final Class<? extends T> cls;

		public NewInstance(Class<? extends T> cls) {
			this.cls = cls;
		}

		@Override
		public T getInterface() {
			try {
				return cls.newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return "NewInstanceProvider [cls=" + cls + "]";
		}

	}

	public static class Singleton<T> extends ApiInstanceProvider<T> {
		private final T obj;

		public Singleton(T obj) {
			this.obj = obj;
		}

		@Override
		public T getInterface() {
			return obj;
		}

		@Override
		public String toString() {
			return "SingletonProvider [obj=" + obj + "]";
		}

	}

	public abstract T getInterface();
}