package openmods.sync;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
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
				.disableSaving()
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

	public static class RegistrationContext {

		private IForgeRegistry<SyncableObjectType> registry;

		public RegistrationContext(IForgeRegistry<SyncableObjectType> registry) {
			this.registry = registry;
		}

		public <T extends ISyncableObject> RegistrationContext register(final ResourceLocation id, Class<T> cls, final Supplier<T> ctor) {
			registry.register(new SyncableObjectType() {
				@Override
				public Class<? extends ISyncableObject> getObjectClass() {
					return cls;
				}

				@Override
				public ISyncableObject createDummyObject() {
					return ctor.get();
				}

				@Override
				public String toString() {
					return "Wrapper{" + cls + "}";
				}
			}.setRegistryName(id));

			return this;
		}

	}

}
