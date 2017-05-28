package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;

public class SyncableObjectTypeRegistry {

	private static class Callbacks extends CommonRegistryCallbacks<Class<? extends ISyncableObject>, SyncableObjectType> {
		@Override
		protected Class<? extends ISyncableObject> getWrappedObject(SyncableObjectType entry) {
			return entry.getObjectClass();
		}
	}

	private static final IForgeRegistry<SyncableObjectType> REGISTRY = new RegistryBuilder<SyncableObjectType>()
			.addCallback(new Callbacks())
			.setIDRange(0, 0xFFFF)
			.setType(SyncableObjectType.class)
			.setName(OpenMods.location("syncable_object_type"))
			.create();

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

	public static void register(Class<? extends ISyncableObject> cls) {
		register(getDomain(), cls);
	}

	public static void register(Class<? extends ISyncableObject> cls, Supplier<ISyncableObject> supplier) {
		register(getDomain(), cls, supplier);
	}

	private static String getDomain() {
		ModContainer mc = Loader.instance().activeModContainer();
		Preconditions.checkState(mc != null, "This method can be only used during mod initialization");
		String prefix = mc.getModId().toLowerCase();
		return prefix;
	}

	public static void register(String domain, Class<? extends ISyncableObject> cls) {
		Preconditions.checkState(!Modifier.isAbstract(cls.getModifiers()), "Class %s is abstract", cls);

		final Constructor<? extends ISyncableObject> ctor;
		try {
			ctor = cls.getConstructor();
		} catch (Exception e) {
			throw new IllegalArgumentException("Class " + cls + " has no parameterless constructor");
		}

		register(domain, cls, new Supplier<ISyncableObject>() {
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

	public static void register(String domain, final Class<? extends ISyncableObject> cls, final Supplier<ISyncableObject> supplier) {
		final ResourceLocation typeId = new ResourceLocation(domain, cls.getName());

		REGISTRY.register(new SyncableObjectType() {

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
	}

}
