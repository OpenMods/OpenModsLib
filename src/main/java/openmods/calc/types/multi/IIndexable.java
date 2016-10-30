package openmods.calc.types.multi;

import com.google.common.base.Optional;

public interface IIndexable {
	public Optional<TypedValue> get(TypedValue index);
}
