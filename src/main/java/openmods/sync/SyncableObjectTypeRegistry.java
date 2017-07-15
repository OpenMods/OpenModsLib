package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;
import openmods.utils.RegistrationContextBase;

@EventBusSubscriber
public class SyncableObjectTypeRegistry {

	private static IForgeRegistry<SyncableObjectType> REGISTRY;

	private static class Callbacks extends CommonRegistryCallbacks<Class<? extends ISyncableObject>, SyncableObjectType> {
		@Override
		protected Class<? extends ISyncableObject> getWrappedObject(SyncableObjectType entry) {
			return entry.getObjectClass();
		}
	}

	@SubscribeEvent
	public static void registerRegistry(RegistryEvent.NewRegistry e) {
		REGISTRY = new RegistryBuilder<SyncableObjectType>()
				.addCallback(new Callbacks())
				.setIDRange(0, 0xFFFF)
				.setType(SyncableObjectType.class)
				.setName(OpenMods.location("syncable_object_type"))
				.create();

	}

	public static SyncableObjectType getType(int typeId) {
		return CommonRegistryCallbacks.getEntryIdMap(REGISTRY).inverse().get(typeId);
	}

	public static int getTypeId(SyncableObjectType type) {
		return CommonRegistryCallbacks.getEntryIdMap(REGISTRY).get(type);
	}

	public static SyncableObjectType getType(Class<? extends ISyncableObject> cls) {
		final Map<Class<? extends ISyncableObject>, SyncableObjectType> objectToEntryMap = CommonRegistryCallbacks.getObjectToEntryMap(REGISTRY);
		return objectToEntryMap.get(cls);
	}

	public static RegistrationContext startRegistration(IForgeRegistry<SyncableObjectType> registry) {
		return new RegistrationContext(registry);
	}

	public static RegistrationContext startRegistration(IForgeRegistry<SyncableObjectType> registry, String domain) {
		return new RegistrationContext(registry, domain);
	}

	public static class RegistrationContext extends RegistrationContextBase<SyncableObjectType> {

		public RegistrationContext(IForgeRegistry<SyncableObjectType> registry, String domain) {
			super(registry, domain);
		}

		public RegistrationContext(IForgeRegistry<SyncableObjectType> registry) {
			super(registry);
		}

		public RegistrationContext register(Class<? extends ISyncableObject> cls) {
			Preconditions.checkState(!Modifier.isAbstract(cls.getModifiers()), "Class %s is abstract", cls);

			final Constructor<? extends ISyncableObject> ctor;
			try {
				ctor = cls.getConstructor();
			} catch (Exception e) {
				throw new IllegalArgumentException("Class " + cls + " has no parameterless constructor");
			}

			return register(cls, new Supplier<ISyncableObject>() {
				@Override
				public ISyncableObject get() {
					try {
						return ctor.newInstance();
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				}
			});
		}

		public RegistrationContext register(final Class<? extends ISyncableObject> cls, final Supplier<ISyncableObject> supplier) {
			final ResourceLocation typeId = new ResourceLocation(domain, cls.getName());

			registry.register(new SyncableObjectType() {

				@Override
				public Class<? extends ISyncableObject> getObjectClass() {
					return cls;
				}

				@Override
				public ISyncableObject createDummyObject() {
					return supplier.get();
				}

				@Override
				public String toString() {
					return "Wrapper{" + cls + "}";
				}
			}.setRegistryName(typeId));

			return this;
		}

	}

}
