package openmods.access;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ApiProviderRegistry<A> {

	private final Class<? super A> markerType;
	private final Map<Class<? extends A>, ApiInstanceProvider<?>> providers = Maps.newHashMap();

	private boolean isFrozen;

	private static boolean shouldIncludeSuper(ApiSingleton meta) {
		return meta == null || meta.includeSuper();
	}

	public void freeze() {
		this.isFrozen = true;
	}

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

	private <T extends A> void registerInterfaces(Class<? extends T> cls, ApiInstanceProvider<T> provider, boolean includeSuper) {
		Set<Class<? extends A>> implemented = Sets.newHashSet();
		findAllImplementedApis(implemented, cls.getInterfaces());
		if (includeSuper) addAllApiInterfaces(implemented);

		for (Class<? extends A> impl : implemented) {
			ApiInstanceProvider<?> prev = providers.put(impl, provider);
			Preconditions.checkState(prev == null, "Conflict on %s: %s -> %s", impl, prev, provider);
		}
	}

	private static boolean shouldIncludeSuper(ApiImplementation meta) {
		return meta == null || meta.includeSuper();
	}

	private static boolean isCacheable(ApiImplementation meta) {
		return meta == null || meta.cacheable();
	}

	public ApiProviderRegistry(Class<? super A> markerType) {
		this.markerType = markerType;
	}

	public boolean isFrozen() {
		return isFrozen;
	}

	public <T extends A> void registerClass(Class<? extends T> cls) {
		Preconditions.checkState(!isFrozen, "This registry is already frozen");

		Preconditions.checkArgument(!Modifier.isAbstract(cls.getModifiers()));

		final ApiImplementation meta = cls.getAnnotation(ApiImplementation.class);

		ApiInstanceProvider<T> provider = isCacheable(meta)? new ApiInstanceProvider.CachedInstance<T>(cls) : new ApiInstanceProvider.NewInstance<T>(cls);
		registerInterfaces(cls, provider, shouldIncludeSuper(meta));
	}

	public <T extends A> void registerInstance(T obj) {
		Preconditions.checkState(!isFrozen, "This registry is already frozen");

		@SuppressWarnings("unchecked")
		final Class<? extends T> cls = (Class<? extends T>)obj.getClass();

		final ApiSingleton meta = cls.getAnnotation(ApiSingleton.class);

		ApiInstanceProvider<T> provider = new ApiInstanceProvider.Singleton<T>(obj);
		registerInterfaces(cls, provider, shouldIncludeSuper(meta));
	}

	@SuppressWarnings("unchecked")
	public <T extends A> T getApi(Class<T> cls) {
		ApiInstanceProvider<?> provider = providers.get(cls);
		Preconditions.checkNotNull(provider, "Can't get implementation for class %s", cls);
		return (T)provider.getInterface();
	}

	public <T extends A> boolean isApiPresent(Class<T> cls) {
		return providers.containsKey(cls);
	}

}