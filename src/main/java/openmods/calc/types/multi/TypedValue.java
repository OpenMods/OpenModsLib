package openmods.calc.types.multi;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class TypedValue {

	public final TypeDomain domain;

	public final Class<?> type;

	public final Object value;

	TypedValue(TypeDomain domain, Class<?> type, Object value) {
		Preconditions.checkArgument(type.isInstance(value), "Value '%s' is not instance of '%s'", value, type);
		this.domain = domain;
		this.type = type;
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null)? 0 : domain.hashCode());
		result = prime * result + ((type == null)? 0 : type.hashCode());
		result = prime * result + ((value == null)? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof TypedValue) {
			final TypedValue other = (TypedValue)obj;
			return this.domain == other.domain &&
					this.type == other.type &&
					Objects.equal(this.value, other.value);
		}

		return false;
	}

	@Override
	public String toString() {
		return "[" + type + ":" + value + "]";
	}

	public TypedValue cast(Class<?> type) {
		return domain.convert(this, type);
	}

	public <T> T unwrap(Class<? extends T> type) {
		return domain.unwrap(this, type);
	}
}
