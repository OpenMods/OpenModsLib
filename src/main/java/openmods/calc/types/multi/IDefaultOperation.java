package openmods.calc.types.multi;

import com.google.common.base.Optional;

public interface IDefaultOperation {
	public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right);
}
