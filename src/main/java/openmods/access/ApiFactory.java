package openmods.access;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.forgespi.language.ModFileScanData;
import openmods.Log;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objectweb.asm.Type;

public class ApiFactory {
	public static final ApiFactory instance = new ApiFactory();

	private interface Setter {
		Class<?> getType();

		void set(Object value) throws Exception;
	}

	private static class ClassInfo {
		private final Map<String, Setter> setters = Maps.newHashMap();

		public ClassInfo(Class<?> cls) {
			for (final Field f : cls.getDeclaredFields()) {
				final int modifiers = f.getModifiers();
				if (Modifier.isStatic(modifiers)) {
					final Class<?> type = f.getType();
					if (Modifier.isFinal(modifiers)) {
						FieldUtils.removeFinalModifier(f);
					} else {
						f.setAccessible(true);
					}
					setters.put(f.getName(), new Setter() {
						@Override
						public void set(Object value) throws Exception {
							f.set(null, value);
						}

						@Override
						public Class<?> getType() {
							return type;
						}
					});
				}
			}

			for (final Method m : cls.getDeclaredMethods()) {
				final int modifiers = m.getModifiers();
				if (Modifier.isStatic(modifiers) && m.getReturnType() == void.class) {
					final Class<?>[] params = m.getParameterTypes();

					if (params.length == 1) {
						final Class<?> param = params[0];
						m.setAccessible(true);
						final String id = m.getName() + Type.getMethodDescriptor(m);
						setters.put(id, new Setter() {
							@Override
							public void set(Object value) throws Exception {
								m.invoke(null, value);
							}

							@Override
							public Class<?> getType() {
								return param;
							}
						});
					}
				}
			}
		}

		public Setter get(String name) {
			return setters.get(name);
		}
	}

	private static ClassInfo resolveType(final Type type) {
		try {
			final Class<?> cls = Class.forName(type.getClassName(), true, ApiFactory.class.getClassLoader());
			return new ClassInfo(cls);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private final Set<Class<? extends Annotation>> apis = Sets.newHashSet();

	private static <A> void fillTargetField(Map<Type, ClassInfo> clsCache, ApiProviderRegistry<A> registry, ModFileScanData.AnnotationData data, Class<A> interfaceMarker) {
		final Type targetClass = data.getClassType();
		final String targetMember = data.getMemberName();

		Preconditions.checkArgument(data.getTargetType() == ElementType.FIELD, "API annotation allowed only on field, is on %s:%s", targetClass, targetMember);

		final ClassInfo targetCls = clsCache.computeIfAbsent(targetClass, ApiFactory::resolveType);
		final Setter setter = targetCls.get(targetMember);
		Preconditions.checkArgument(setter != null, "Entry '%s' in class '%s' is not valid target for API annotation", targetMember, targetClass);

		final Class<?> acceptedType = setter.getType();
		Preconditions.checkState(interfaceMarker.isAssignableFrom(acceptedType), "Failed to set API object on %s:%s - invalid type, expected %s",
				targetClass, targetMember, interfaceMarker);

		final Class<? extends A> castAcceptedType = acceptedType.asSubclass(interfaceMarker);
		final A api = registry.getApi(castAcceptedType);

		if (api != null) {
			try {
				setter.set(api);
			} catch (Throwable t) {
				throw new RuntimeException(String.format("Failed to set entry '%s' in class '%s'", targetMember, targetClass), t);
			}
			Log.trace("Injecting instance of %s from mod %s to field %s:%s from file %s",
					castAcceptedType,
					ModLoadingContext.get().getActiveContainer().getModId(),
					targetClass,
					targetMember);
		} else {
			Log.info("Can't set API field %s:%s - no API for type %s",
					targetClass, targetMember, castAcceptedType);
		}
	}

	private static <A> void fillTargetFields(final ApiProviderRegistry<A> registry, Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker) {
		final Type marker = Type.getType(fieldMarker);
		ModList.get().getAllScanData().stream()
				.flatMap(s -> s.getAnnotations().stream())
				.filter(d -> marker.equals(d.getAnnotationType()))
				.forEach(data -> fillTargetField(Maps.newHashMap(), registry, data, interfaceMarker));
	}

	public interface ApiProviderSetup<A> {
		void setup(ApiProviderRegistry<A> registry);
	}

	public <A> ApiProviderRegistry<A> createApi(Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker, ApiProviderSetup<A> registrySetup) {
		Preconditions.checkState(apis.add(fieldMarker), "Duplicate API registration on %s", fieldMarker);

		final ApiProviderRegistry<A> registry = new ApiProviderRegistry<>(interfaceMarker);
		registrySetup.setup(registry);
		registry.freeze();

		fillTargetFields(registry, fieldMarker, interfaceMarker);

		return registry;
	}

	public <A> void createApi(Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker, ApiProviderRegistry<A> registry) {
		Preconditions.checkState(apis.add(fieldMarker), "Duplicate API registration on %s", fieldMarker);

		Preconditions.checkState(registry.isFrozen(), "Registry must be frozen");
		fillTargetFields(registry, fieldMarker, interfaceMarker);
	}
}
