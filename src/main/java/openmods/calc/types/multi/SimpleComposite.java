package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;

public abstract class SimpleComposite implements IComposite {

	private final Map<Class<? extends ICompositeTrait>, Boolean> traitsCache = Maps.newIdentityHashMap();

	@Override
	public boolean has(Class<? extends ICompositeTrait> cls) {
		Boolean hasTrait = traitsCache.get(cls);
		if (hasTrait == null) {
			hasTrait = cls.isInstance(this);
			traitsCache.put(cls, hasTrait);
		}

		return hasTrait;
	}

	@Override
	public <T extends ICompositeTrait> T get(Class<T> cls) {
		Preconditions.checkState(has(cls));
		return cls.cast(this);
	}

	@Override
	public <T extends ICompositeTrait> Optional<T> getOptional(Class<T> cls) {
		return has(cls)? Optional.of(cls.cast(this)) : Optional.<T> absent();
	}

}
