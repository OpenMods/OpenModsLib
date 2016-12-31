package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import openmods.calc.types.multi.TypeDomain.Coercion;

public class TypedValueComparator implements Comparator<TypedValue> {

	@Override
	public int compare(TypedValue left, TypedValue right) {
		final TypeDomain domain = left.domain;
		Preconditions.checkArgument(domain == right.domain, "Incompatible domains for values: %s and %s", left, right);

		final Class<?> type;
		final Coercion coercionRule = domain.getCoercionRule(left.type, right.type);
		if (coercionRule == Coercion.TO_LEFT) {
			type = left.type;
		} else if (coercionRule == Coercion.TO_RIGHT) {
			type = right.type;
		} else throw new IllegalArgumentException("Can't compare " + left + " and " + right);

		return compareTyped(type, left, right);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> int compareTyped(Class<?> type, TypedValue left, TypedValue right) {
		Preconditions.checkArgument(Comparable.class.isAssignableFrom(type), "Type %s is not comparable", type);
		return compare((Class<T>)type, left, right);
	}

	private static <T extends Comparable<T>> int compare(Class<T> cls, TypedValue left, TypedValue right) {
		final T l = left.unwrap(cls);
		final T r = right.unwrap(cls);

		return l.compareTo(r);
	}
}
