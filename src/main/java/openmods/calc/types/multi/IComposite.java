package openmods.calc.types.multi;

import com.google.common.base.Optional;

public interface IComposite {

	public String type();

	public boolean has(Class<? extends ICompositeTrait> cls);

	public <T extends ICompositeTrait> T get(Class<T> cls);

	public <T extends ICompositeTrait> Optional<T> getOptional(Class<T> cls);

}
