package openmods.config.game;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import java.util.Map;

public class FactoryRegistry<T> {

	public interface Factory<T> {
		public T construct();
	}

	private final Map<String, Factory<T>> customFactories = Maps.newHashMap();

	public void registerFactory(String feature, Factory<T> factory) {
		customFactories.put(feature, factory);
	}

	public <C extends T> C construct(String feature, Class<? extends C> cls) {
		Factory<T> customFactory = customFactories.get(feature);
		if (customFactory != null) {
			@SuppressWarnings("unchecked")
			C result = (C)customFactory.construct();
			Preconditions.checkArgument(cls.isInstance(result),
					"Invalid class for feature entry '%s', got '%s', expected '%s'",
					feature, result != null? result.getClass().toString() : "null", cls);
			return result;
		}

		try {
			return cls.newInstance();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
}
