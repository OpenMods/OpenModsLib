package openmods.calc.types.multi;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class TypedValue {

	public final TypeDomain domain;

	public final Class<?> type;

	public final Object value;

	private Optional<Boolean> truthyCache;

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

	public String typeStr() {
		return domain.getName(type);
	}

	@Override
	public String toString() {
		return "[" + typeStr() + ":" + value + "]";
	}

	public TypedValue cast(Class<?> type) {
		return domain.convert(this, type);
	}

	public <T> T unwrap(Class<T> type) {
		return domain.unwrap(this, type);
	}

	private String getClassName(Class<?> cls) {
		final Optional<String> name = domain.tryGetName(cls);
		return name.isPresent()? name.get() : cls.getSimpleName();
	}

	private ClassCastException castException(Class<?> expectedType) {
		return new ClassCastException(String.format("Expected '%s', got %s", getClassName(expectedType), this));
	}

	private ClassCastException castException(Class<?> expectedType, String location) {
		return new ClassCastException(String.format("Expected '%s' on %s, got %s", getClassName(expectedType), location, this));
	}

	public <T> T as(Class<T> expectedType) {
		try {
			return expectedType.cast(value);
		} catch (ClassCastException e) {
			throw castException(expectedType);
		}
	}

	public <T> T as(Class<T> expectedType, String location) {
		try {
			return expectedType.cast(value);
		} catch (ClassCastException e) {
			throw castException(expectedType, location);
		}
	}

	public void checkType(Class<?> expectedType) {
		if (!expectedType.isInstance(value)) throw castException(expectedType);
	}

	public void checkType(Class<?> expectedType, String location) {
		if (!expectedType.isInstance(value)) throw castException(expectedType, location);
	}

	public Optional<Boolean> isTruthyOptional() {
		if (truthyCache == null) truthyCache = domain.isTruthy(this);
		return truthyCache;
	}

	public static class NoLogicValueException extends RuntimeException {
		private static final long serialVersionUID = -5318443217371834267L;

		public NoLogicValueException(TypedValue value) {
			super(String.format("Value %s is neither true or false", value));
		}
	}

	public boolean isTruthy() {
		final Optional<Boolean> isTruthy = isTruthyOptional();
		if (!isTruthy.isPresent()) throw new NoLogicValueException(this);
		return isTruthy.get();
	}

	public boolean is(Class<?> type) {
		return this.type == type;
	}
}
