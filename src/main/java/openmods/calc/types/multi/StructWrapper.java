package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
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
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface ExposeMethod {}

	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface ExposeProperty {
		public boolean raw() default false;
	}

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
			final ImmutableMap.Builder<String, MemberValueProvider> members = ImmutableMap.builder();

			for (Field f : cls.getFields()) {
				final ExposeProperty annotation = f.getAnnotation(ExposeProperty.class);
				if (annotation != null) {
					if (annotation.raw()) {
						appendRawFieldMember(members, f);
					} else {
						appendFieldMember(members, f);
					}
				}
			}

			final Multimap<String, Method> methods = HashMultimap.create();

			for (final Method m : cls.getMethods()) {
				final ExposeProperty annotation = m.getAnnotation(ExposeProperty.class);
				if (annotation != null) {
					if (annotation.raw()) {
						appendRawGetterMember(members, m);
					} else {
						appendGetterMember(members, m);
					}
				} else if (m.isAnnotationPresent(ExposeMethod.class)) {
					methods.put(m.getName(), m);
				}
			}

			for (Map.Entry<String, Collection<Method>> e : methods.asMap().entrySet()) {
				final TypedFunction.Builder builder = TypedFunction.builder();
				for (Method m : e.getValue())
					builder.addVariant(m);

				appendFunctionMember(members, e.getKey(), builder.build(cls));
			}

			return members.build();
		}
	};

	private static void appendFunctionMember(ImmutableMap.Builder<String, MemberValueProvider> members, final String name, final IUnboundCallable function) {
		members.put(name, new MemberValueProvider() {
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

	private static void appendGetterMember(ImmutableMap.Builder<String, MemberValueProvider> members, final Method m) {
		Preconditions.checkState(m.getParameterTypes().length == 0, "Getter method must have no parameters");
		m.setAccessible(true);
		members.put(m.getName(), new MemberValueProvider() {
			@Override
			public TypedValue getValue(TypeDomain domain, Object target) {
				return wrapMethodValue(m, domain, target);
			}

			@SuppressWarnings("unchecked")
			private <T> TypedValue wrapMethodValue(Method m, TypeDomain domain, Object target) {
				try {
					final T value = (T)m.invoke(target);
					final Class<T> cls = (Class<T>)m.getReturnType();
					return domain.create(cls, value);
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			}
		});
	}

	private static void appendRawGetterMember(Builder<String, MemberValueProvider> members, final Method m) {
		Preconditions.checkState(m.getParameterTypes().length == 0, "Getter method must have no parameters");
		Preconditions.checkState(m.getReturnType() == TypedValue.class, "Raw getter must return TypedValue");
		m.setAccessible(true);

		members.put(m.getName(), new MemberValueProvider() {
			@Override
			public TypedValue getValue(TypeDomain domain, Object target) {
				try {
					return (TypedValue)m.invoke(target);
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			}
		});
	}

	private static void appendFieldMember(ImmutableMap.Builder<String, MemberValueProvider> members, Field f) {
		final FieldAccess<?> field = FieldAccess.create(f);
		members.put(f.getName(), new MemberValueProvider() {
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

	private static void appendRawFieldMember(Builder<String, MemberValueProvider> members, Field f) {
		Preconditions.checkState(f.getType() == TypedValue.class, "Invalid field %s type", f);
		final FieldAccess<TypedValue> field = FieldAccess.create(f);
		members.put(f.getName(), new MemberValueProvider() {
			@Override
			public TypedValue getValue(TypeDomain domain, Object target) {
				return field.get(target);
			}
		});
	}

	public static <T> StructWrapper create(Class<? super T> cls, T target) {
		final Map<String, MemberValueProvider> members = membersCache.getOrCreate(cls);
		return new StructWrapper(members, target);
	}

	public static <T> TypedValue create(TypeDomain domain, Class<? super T> cls, T target) {
		return domain.create(StructWrapper.class, create(cls, target));
	}

	public static StructWrapper create(Object target) {
		final Map<String, MemberValueProvider> members = membersCache.getOrCreate(target.getClass());
		return new StructWrapper(members, target);
	}

	public static TypedValue create(TypeDomain domain, Object target) {
		return domain.create(StructWrapper.class, create(target));
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
