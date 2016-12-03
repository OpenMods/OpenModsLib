package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class MappedComposite implements IComposite {

	private final String type;

	private final Map<Class<? extends ICompositeTrait>, ICompositeTrait> traits;

	public MappedComposite(String type, Map<Class<? extends ICompositeTrait>, ICompositeTrait> traits) {
		this.type = type;
		this.traits = traits;
	}

	@Override
	public boolean has(Class<? extends ICompositeTrait> cls) {
		return traits.containsKey(cls);
	}

	@Override
	public <T extends ICompositeTrait> T get(Class<T> cls) {
		final ICompositeTrait trait = traits.get(cls);
		Preconditions.checkState(trait != null, "No trait: %s", cls);
		return cls.cast(trait);
	}

	@Override
	public <T extends ICompositeTrait> Optional<T> getOptional(Class<T> cls) {
		final ICompositeTrait trait = traits.get(cls);
		if (trait == null) return Optional.absent();
		return Optional.of(cls.cast(trait));
	}

	@Override
	public String type() {
		return type;
	}

	public static class Builder {
		private Builder() {}

		private final ImmutableMap.Builder<Class<? extends ICompositeTrait>, ICompositeTrait> traits = ImmutableMap.builder();

		public <T extends ICompositeTrait> Builder put(Class<T> trait, T target) {
			traits.put(trait, target);
			return this;
		}

		public IComposite build(String type) {
			return new MappedComposite(type, traits.build());
		}
	}

	public static Builder builder() {
		return new Builder();
	}

}
