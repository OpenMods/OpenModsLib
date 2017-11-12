package openmods.access;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import openmods.Log;
import openmods.utils.CachedFactory;
import org.objectweb.asm.Type;

public class ApiFactory {

	public static final ApiFactory instance = new ApiFactory();

	private interface Setter {
		public Class<?> getType();

		public void set(Object value) throws Exception;
	}

	private static class ClassInfo {
		private Map<String, Setter> setters = Maps.newHashMap();

		public ClassInfo(Class<?> cls) {
			for (final Field f : cls.getDeclaredFields()) {
				final int modifiers = f.getModifiers();
				if (Modifier.isStatic(modifiers)) {
					final Class<?> type = f.getType();
					f.setAccessible(true);
					if (Modifier.isFinal(modifiers)) {
						setters.put(f.getName(), new Setter() {
							@Override
							public void set(Object value) throws Exception {
								EnumHelper.setFailsafeFieldValue(f, null, value);
							}

							@Override
							public Class<?> getType() {
								return type;
							}
						});
					} else {
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

	private static class ClassInfoCache extends CachedFactory<String, ClassInfo> {
		@Override
		protected ClassInfo create(String className) {
			try {
				final Class<?> cls = Class.forName(className, true, getClass().getClassLoader());
				return new ClassInfo(cls);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final Set<Class<? extends Annotation>> apis = Sets.newHashSet();

	private static <A> void fillTargetField(ClassInfoCache clsCache, ApiProviderRegistry<A> registry, ASMData data, Class<A> interfaceMarker) {
		final String targetClassName = data.getClassName();
		final String targetObjectName = data.getObjectName();

		final ClassInfo targetCls = clsCache.getOrCreate(targetClassName);
		final Setter setter = targetCls.get(targetObjectName);
		Preconditions.checkArgument(setter != null, "Entry '%s' in class '%s' is not valid target for API annotation", targetObjectName, targetClassName);

		final Class<?> acceptedType = setter.getType();
		Preconditions.checkState(interfaceMarker.isAssignableFrom(acceptedType), "Failed to set API object on %s:%s - invalid type, expected %s",
				targetClassName, targetObjectName, interfaceMarker);

		final Class<? extends A> castAcceptedType = acceptedType.asSubclass(interfaceMarker);
		final A api = registry.getApi(castAcceptedType);

		if (api != null) {
			try {
				setter.set(api);
			} catch (Throwable t) {
				throw new RuntimeException(String.format("Failed to set entry '%s' in class '%s'", targetObjectName, targetClassName), t);
			}
			Log.trace("Injecting instance of %s from mod %s to field %s:%s from file %s",
					castAcceptedType,
					Loader.instance().activeModContainer().getModId(),
					targetClassName,
					targetObjectName,
					data.getCandidate().getModContainer());
		} else {
			Log.info("Can't set API field %s:%s - no API for type %s",
					targetClassName, targetObjectName, castAcceptedType);
		}
	}

	private static <A> void fillTargetFields(final ApiProviderRegistry<A> registry, ASMDataTable table, Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker) {
		final ClassInfoCache clsCache = new ClassInfoCache();
		final Set<ASMData> targets = table.getAll(fieldMarker.getName());

		for (ASMData data : targets)
			fillTargetField(clsCache, registry, data, interfaceMarker);
	}

	public interface ApiProviderSetup<A> {
		public void setup(ApiProviderRegistry<A> registry);
	}

	public <A> ApiProviderRegistry<A> createApi(Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker, ASMDataTable table, ApiProviderSetup<A> registrySetup) {
		Preconditions.checkState(apis.add(fieldMarker), "Duplicate API registration on %s", fieldMarker);

		final ApiProviderRegistry<A> registry = new ApiProviderRegistry<>(interfaceMarker);
		registrySetup.setup(registry);
		registry.freeze();

		fillTargetFields(registry, table, fieldMarker, interfaceMarker);

		return registry;
	}

	public <A> void createApi(Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker, ASMDataTable table, ApiProviderRegistry<A> registry) {
		Preconditions.checkState(apis.add(fieldMarker), "Duplicate API registration on %s", fieldMarker);

		Preconditions.checkState(registry.isFrozen(), "Registry must be frozen");
		fillTargetFields(registry, table, fieldMarker, interfaceMarker);
	}
}
