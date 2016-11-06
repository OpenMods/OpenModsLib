package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SingleTraitComposite implements IComposite {

	private final String type;

	private final ICompositeTrait trait;

	public SingleTraitComposite(String type, ICompositeTrait trait) {
		this.type = type;
		this.trait = trait;
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public boolean has(Class<? extends ICompositeTrait> cls) {
		return cls.isInstance(trait);
	}

	@Override
	public <E extends ICompositeTrait> E get(Class<E> cls) {
		Preconditions.checkState(has(cls), "No trait: %s", cls);
		return cls.cast(trait);
	}

	@Override
	public <T extends ICompositeTrait> Optional<T> getOptional(Class<T> cls) {
		return has(cls)? Optional.of(get(cls)) : Optional.<T> absent();
	}

}
