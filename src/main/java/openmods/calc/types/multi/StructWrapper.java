package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.types.multi.TypedFunction.IUnboundCallable;
import openmods.reflection.FieldAccess;
import openmods.utils.CachedFactory;
import openmods.utils.OptionalInt;

public class StructWrapper {
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Expose {}

	private interface MemberValueProvider {
		public TypedValue getValue(TypeDomain domain, Object target);
	}

	private final Map<String, MemberValueProvider> members;

	private final Object target;

	private StructWrapper(Map<String, MemberValueProvider> members, Object target) {
		this.members = members;
		this.target = target;
	}

	public Optional<TypedValue> getValue(TypeDomain domain, String key) {
		final MemberValueProvider valueProvider = members.get(key);
		if (valueProvider == null) return Optional.absent();
		return Optional.of(valueProvider.getValue(domain, target));
	}

	private static final CachedFactory<Class<?>, Map<String, MemberValueProvider>> membersCache = new CachedFactory<Class<?>, Map<String, MemberValueProvider>>() {

		@Override
		protected Map<String, MemberValueProvider> create(Class<?> cls) {
			final Map<String, MemberValueProvider> result = Maps.newHashMap();

			for (Field f : cls.getFields()) {
				if (f.isAnnotationPresent(Expose.class)) {
					final FieldAccess<?> field = FieldAccess.create(f);
					result.put(f.getName(), new MemberValueProvider() {
						@Override
						public TypedValue getValue(TypeDomain domain, Object target) {
							return wrapFieldValue(field, domain, target);
						}

						private <T> TypedValue wrapFieldValue(FieldAccess<T> field, TypeDomain domain, Object target) {
							final T value = field.get(target);
							return domain.create(field.getType(), value);
						}
					});
				}
			}

			final Multimap<String, Method> methods = HashMultimap.create();

			for (Method m : cls.getMethods())
				if (m.isAnnotationPresent(Expose.class))
					methods.put(m.getName(), m);

			for (Map.Entry<String, Collection<Method>> e : methods.asMap().entrySet()) {
				final TypedFunction.Builder builder = TypedFunction.builder();
				for (Method m : e.getValue())
					builder.addVariant(m);

				final IUnboundCallable function = builder.build(cls);

				result.put(e.getKey(), new MemberValueProvider() {

					@Override
					public TypedValue getValue(final TypeDomain domain, final Object target) {
						return domain.create(CallableValue.class, new CallableValue() {
							@Override
							public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
								function.call(domain, target, frame, argumentsCount, returnsCount);
							}
						});
					}
				});
			}

			return ImmutableMap.copyOf(result);
		}

	};

	public static <T> TypedValue create(TypeDomain domain, Class<? super T> cls, T target) {
		final Map<String, MemberValueProvider> members = membersCache.getOrCreate(cls);
		return domain.create(StructWrapper.class, new StructWrapper(members, target));
	}

	public static TypedValue create(TypeDomain domain, Object target) {
		final Map<String, MemberValueProvider> members = membersCache.getOrCreate(target.getClass());
		return domain.create(StructWrapper.class, new StructWrapper(members, target));
	}

	public static void register(Environment<TypedValue> env) {
		final TypedValue nullValue = env.nullValue();
		final TypeDomain domain = nullValue.domain;

		final TypedValue structType = domain.create(TypeUserdata.class, new TypeUserdata("object", StructWrapper.class));

		env.setGlobalSymbol("object", structType);

		domain.registerType(StructWrapper.class, "object",
				MetaObject.builder()
						.set(new MetaObject.SlotAttr() {
							@Override
							public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
								final TypeDomain domain = self.domain;
								return self.as(StructWrapper.class).getValue(domain, key);
							}
						})
						.build());
	}
}
