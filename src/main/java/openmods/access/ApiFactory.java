package openmods.access;

import java.lang.annotation.Annotation;
import java.util.Set;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import openmods.Log;
import openmods.reflection.FieldAccess;
import openmods.utils.CachedFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

public class ApiFactory {

	public static final ApiFactory instance = new ApiFactory();

	private final CachedFactory<String, Class<?>> clsCache = new CachedFactory<String, Class<?>>() {
		@Override
		protected Class<?> create(String className) {
			try
			{
				return Class.forName(className, true, getClass().getClassLoader());
			}
			catch (Exception ex)
			{
				throw Throwables.propagate(ex);
			}
		}
	};

	private final Set<Class<? extends Annotation>> apis = Sets.newHashSet();

	private <A> void fillTargetField(final ApiProviderRegistry<A> registry, ASMData data, Class<A> interfaceMarker) {
		final String targetClassName = data.getClassName();
		final String targetFieldName = data.getObjectName();

		final Class<?> targetCls = clsCache.getOrCreate(targetClassName);
		final FieldAccess<?> field = FieldAccess.create(targetCls, targetFieldName);
		final Class<?> type = field.getType();

		Preconditions.checkState(field.isStatic(), "Failed to set API field on %s:%s - field must be static",
				targetClassName, targetFieldName);

		Preconditions.checkState(!field.isFinal(), "Failed to set API field on %s:%s - field must not be final",
				targetClassName, targetFieldName);

		Preconditions.checkState(interfaceMarker.isAssignableFrom(type), "Failed to set API field on %s:%s - invalid type, expected %s",
				targetClassName, targetFieldName, interfaceMarker);

		final FieldAccess<A> castedField = field.cast(interfaceMarker);
		final A api = registry.getApi(castedField.getType());

		if (api != null) {
			castedField.set(null, api);
			Log.trace("Injecting instance of %s from mod %s to field %s:%s from file %s",
					type,
					Loader.instance().activeModContainer().getModId(),
					targetClassName,
					targetFieldName,
					data.getCandidate().getModContainer()
					);
		} else {
			Log.info("Can't set API field %s:%s - no API for type %s",
					targetClassName, targetFieldName, castedField.getType());
		}
	}

	private <A> void fillTargetFields(final ApiProviderRegistry<A> registry, ASMDataTable table, Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker) {
		final Set<ASMData> targets = table.getAll(fieldMarker.getName());

		for (ASMData data : targets)
			fillTargetField(registry, data, interfaceMarker);
	}

	public interface ApiProviderSetup<A> {
		public void setup(ApiProviderRegistry<A> registry);
	}

	public <A> ApiProviderRegistry<A> createApi(Class<? extends Annotation> fieldMarker, Class<A> interfaceMarker, ASMDataTable table, ApiProviderSetup<A> registrySetup) {
		Preconditions.checkState(apis.add(fieldMarker), "Duplicate API registration on %s", fieldMarker);

		final ApiProviderRegistry<A> registry = new ApiProviderRegistry<A>(interfaceMarker);
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
