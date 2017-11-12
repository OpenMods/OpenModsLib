package openmods.access;

import com.google.common.reflect.TypeToken;

public abstract class ApiProviderBase<A> {

	@SuppressWarnings("serial")
	private final Class<? super A> markerType = (new TypeToken<A>(getClass()) {}).getRawType();

	private final ApiProviderRegistry<A> apiRegistry;

	public ApiProviderBase(ApiProviderRegistry<A> apiRegistry) {
		this.apiRegistry = apiRegistry;
	}

	public ApiProviderBase() {
		this.apiRegistry = new ApiProviderRegistry<>(markerType);
	}

	public void registerClass(Class<? extends A> cls) {
		apiRegistry.registerClass(cls);
	}

	public <T extends A> void registerInstance(T obj) {
		apiRegistry.registerInstance(obj);
	}

	public <T extends A> T getApi(Class<T> cls) {
		return apiRegistry.getApi(cls);
	}

	public <T extends A> boolean isApiPresent(Class<T> cls) {
		return apiRegistry.isApiPresent(cls);
	}

}
