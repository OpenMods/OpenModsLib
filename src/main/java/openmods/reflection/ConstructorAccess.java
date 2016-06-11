package openmods.reflection;

import java.lang.reflect.Constructor;
import openmods.serializable.IInstanceFactory;
import openmods.utils.SneakyThrower;

public class ConstructorAccess<T> implements IInstanceFactory<T> {

	public static class ConstructorAccessException extends RuntimeException {
		private static final long serialVersionUID = 343341828278770966L;

		private static String createMessage(Constructor<?> ctor) {
			return String.format("Failed to create object of %s via constructor %s", ctor.getDeclaringClass(), ctor);
		}

		public ConstructorAccessException(Constructor<?> ctor, Throwable cause) {
			super(createMessage(ctor), cause);
		}

		public ConstructorAccessException(Constructor<?> ctor) {
			super(createMessage(ctor));
		}

	}

	private Constructor<? extends T> ctor;

	public ConstructorAccess(Constructor<? extends T> ctor) {
		ReflectionLog.logLoad(ctor);
		ctor.setAccessible(true);
		this.ctor = ctor;
	}

	@Override
	public T create() {
		try {
			return ctor.newInstance();
		} catch (Throwable t) {
			throw new ConstructorAccessException(ctor, t);
		}
	}

	public static <T> ConstructorAccess<T> create(Class<? extends T> cls) {
		try {
			Constructor<? extends T> c = cls.getConstructor();
			return new ConstructorAccess<T>(c);
		} catch (Throwable t) {
			throw SneakyThrower.sneakyThrow(t);
		}
	}
}
