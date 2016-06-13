package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public class TypeDomain {

	private interface RawConverter {
		public Object convert(Object value);
	}

	private static final TypeVariable<?> VAR_S;
	private static final TypeVariable<?> VAR_T;

	static {
		final TypeVariable<?>[] typeParameters = IConverter.class.getTypeParameters();
		VAR_S = typeParameters[0];
		VAR_T = typeParameters[1];
	}

	private static class WrappedConverter<S, T> implements RawConverter {
		private final Class<? extends S> source;

		private final IConverter<S, T> converter;

		public WrappedConverter(Class<? extends S> source, IConverter<S, T> converter) {
			this.source = source;
			this.converter = converter;
		}

		@Override
		public Object convert(Object value) {
			S input = source.cast(value);
			T result = converter.convert(input);
			return result;
		}
	}

	private static class CastConverter<T> implements RawConverter {
		private final Class<? extends T> target;

		public CastConverter(Class<? extends T> target) {
			this.target = target;
		}

		@Override
		public Object convert(Object value) {
			return target.cast(value);
		}

	}

	private final Table<Class<?>, Class<?>, RawConverter> converters = HashBasedTable.create();

	public <T> void registerCast(Class<? extends T> source, Class<T> target) {
		final RawConverter prev = converters.put(source, target, new CastConverter<T>(target));
		Preconditions.checkState(prev == null, "Duplicate registration for types (%s,%s)", source, target);
	}

	public <S, T> void registerConverter(Class<? extends S> source, Class<? extends T> target, IConverter<S, T> converter) {
		final RawConverter prev = converters.put(source, target, new WrappedConverter<S, T>(source, converter));
		Preconditions.checkState(prev == null, "Duplicate registration for types (%s,%s)", source, target);
	}

	@SuppressWarnings("unchecked")
	public <S, T> void registerConverter(IConverter<S, T> converter) {
		final TypeToken<?> converterType = TypeToken.of(converter.getClass());
		final Class<S> sourceType = (Class<S>)converterType.resolveType(VAR_S).getRawType();
		final Class<T> targetType = (Class<T>)converterType.resolveType(VAR_T).getRawType();
		registerConverter(sourceType, targetType, converter);
	}

	private RawConverter getConverter(TypedValue value, Class<?> type) {
		final RawConverter converter = converters.get(value.type, type);
		Preconditions.checkArgument(converter != null, "No known conversion from %s to %s", value.type, type);
		return converter;
	}

	private void checkConversion(Class<?> from, Class<?> to) {
		Preconditions.checkArgument(converters.contains(from, to), "No known conversion from %s to %s", from, to);
	}

	public TypedValue convert(TypedValue value, Class<?> type) {
		if (value.type == type) return value;
		final RawConverter converter = getConverter(value, type);
		final Object convertedValue = converter.convert(value.value);
		return new TypedValue(this, type, convertedValue);
	}

	public <T> T unwrap(TypedValue value, Class<? extends T> type) {
		if (value.type == type) return type.cast(value.value);
		final RawConverter converter = getConverter(value, type);
		final Object convertedValue = converter.convert(value.value);
		return type.cast(convertedValue);
	}

	public enum Coercion {
		TO_LEFT, TO_RIGHT, INVALID;
	}

	private static final Map<Coercion, Coercion> inverses = Maps.newEnumMap(Coercion.class);

	static {
		inverses.put(Coercion.TO_LEFT, Coercion.TO_RIGHT);
		inverses.put(Coercion.TO_RIGHT, Coercion.TO_LEFT);
		inverses.put(Coercion.INVALID, Coercion.INVALID);
	}

	private final Table<Class<?>, Class<?>, Coercion> coercionRules = HashBasedTable.create();

	public void registerCoercionRule(Class<?> left, Class<?> right, Coercion rule) {
		Preconditions.checkArgument(left != right);
		if (rule == Coercion.TO_LEFT) {
			checkConversion(right, left);
		} else if (rule == Coercion.TO_RIGHT) {
			checkConversion(left, right);
		}

		final Coercion prev = coercionRules.put(left, right, rule);
		Preconditions.checkState(prev == null || prev == rule, "Duplicate coercion rule for (%s,%s): %s -> %s", left, right, rule);
	}

	public void registerSymmetricCoercionRule(Class<?> left, Class<?> right, Coercion rule) {
		registerCoercionRule(left, right, rule);
		registerCoercionRule(right, left, inverses.get(rule));
	}

	public Coercion getCoercionRule(Class<?> left, Class<?> right) {
		if (left == right) return Coercion.TO_LEFT;
		final Coercion result = coercionRules.get(left, right);
		return result != null? result : Coercion.INVALID;
	}

	public <T> TypedValue create(Class<? super T> type, T value) {
		return new TypedValue(this, type, value);
	}

}
