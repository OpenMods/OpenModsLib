package openmods.access;

import java.lang.reflect.Modifier;
import java.util.*;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

public class ApiProviderBase<A> {

	public interface IApiInstanceProvider<T> {
		public T getInterface();
	}

	private static class SingleInstanceProvider<T> implements IApiInstanceProvider<T> {
		private final T instance;

		public SingleInstanceProvider(Class<? extends T> cls) {
			try {
				instance = cls.newInstance();
			} catch (Throwable t) {
				throw Throwables.propagate(t);
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

	private static class NewInstanceProvider<T> implements IApiInstanceProvider<T> {
		private final Class<? extends T> cls;

		public NewInstanceProvider(Class<? extends T> cls) {
			this.cls = cls;
		}

		@Override
		public T getInterface() {
			try {
				return cls.newInstance();
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}
		}

		@Override
		public String toString() {
			return "NewInstanceProvider [cls=" + cls + "]";
		}

	}

	private static class SingletonProvider<T> implements IApiInstanceProvider<T> {
		private final T obj;

		public SingletonProvider(T obj) {
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

	@SuppressWarnings("serial")
	private final Class<? super A> markerType = (new TypeToken<A>(getClass()) {}).getRawType();

	private final Map<Class<? extends A>, IApiInstanceProvider<?>> providers = Maps.newHashMap();

	@SuppressWarnings("unchecked")
	private void findAllImplementedApis(Collection<Class<? extends A>> output, Class<?>... intfs) {
		for (Class<?> cls : intfs) {
			Preconditions.checkArgument(cls.isInterface());
			if (markerType.isAssignableFrom(cls) && markerType != cls) output.add((Class<? extends A>)cls);
		}
	}

	private void addAllApiInterfaces(Set<Class<? extends A>> interfaces) {
		Queue<Class<? extends A>> queue = Lists.newLinkedList(interfaces);

		Class<? extends A> cls;
		while ((cls = queue.poll()) != null) {
			interfaces.add(cls);
			findAllImplementedApis(queue, cls.getInterfaces());
		}
	}

	private <T extends A> void registerInterfaces(Class<? extends T> cls, IApiInstanceProvider<T> provider, boolean includeSuper) {
		Set<Class<? extends A>> implemented = Sets.newHashSet();
		findAllImplementedApis(implemented, cls.getInterfaces());
		if (includeSuper) addAllApiInterfaces(implemented);

		for (Class<? extends A> impl : implemented) {
			IApiInstanceProvider<?> prev = providers.put(impl, provider);
			Preconditions.checkState(prev == null, "Conflict on %s: %s -> %s", impl, prev, provider);
		}
	}

	public <T extends A> void registerClass(Class<? extends T> cls) {
		Preconditions.checkArgument(!Modifier.isAbstract(cls.getModifiers()));

		ApiImplementation meta = cls.getAnnotation(ApiImplementation.class);
		Preconditions.checkNotNull(meta);

		IApiInstanceProvider<T> provider = meta.cacheable()? new SingleInstanceProvider<T>(cls) : new NewInstanceProvider<T>(cls);
		registerInterfaces(cls, provider, meta.includeSuper());
	}

	public <T extends A> void registerInstance(T obj) {
		@SuppressWarnings("unchecked")
		final Class<? extends T> cls = (Class<? extends T>)obj.getClass();

		ApiSingleton meta = cls.getAnnotation(ApiSingleton.class);
		Preconditions.checkNotNull(meta);

		IApiInstanceProvider<T> provider = new SingletonProvider<T>(obj);
		registerInterfaces(cls, provider, meta.includeSuper());
	}

	@SuppressWarnings("unchecked")
	public <T extends A> T getApi(Class<T> cls) {
		IApiInstanceProvider<?> provider = providers.get(cls);
		Preconditions.checkNotNull(provider, "Can't get implementation for class %s", cls);
		return (T)provider.getInterface();
	}

	public <T extends A> boolean isApiPresent(Class<T> cls) {
		return providers.containsKey(cls);
	}
}
