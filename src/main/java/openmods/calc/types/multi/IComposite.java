package openmods.calc.types.multi;

import com.google.common.base.Optional;

public interface IComposite {

	public Optional<TypedValue> get(TypeDomain domain, String component);

	public String subtype();

}
