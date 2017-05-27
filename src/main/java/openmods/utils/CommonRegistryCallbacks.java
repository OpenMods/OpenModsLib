package openmods.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import openmods.OpenMods;
import openmods.network.rpc.MethodEntry;

public abstract class CommonRegistryCallbacks<T, E extends IForgeRegistryEntry<E>> implements IForgeRegistry.AddCallback<E>, IForgeRegistry.ClearCallback<E>, IForgeRegistry.CreateCallback<E> {

	private static final ResourceLocation OBJECT_TO_ENTRY = OpenMods.location("object_to_entry");

	private static final ResourceLocation ENTRY_TO_ID = OpenMods.location("entry_to_id");

	protected abstract T getWrappedObject(E entry);

	@Override
	@SuppressWarnings("unchecked")
	public void onCreate(Map<ResourceLocation, ?> slaveset, BiMap<ResourceLocation, ? extends IForgeRegistry<?>> registries) {
		final Map<T, E> classToEntryMap = Maps.newHashMap();
		((Map<ResourceLocation, Object>)slaveset).put(OBJECT_TO_ENTRY, classToEntryMap);

		final BiMap<MethodEntry, Integer> entryToId = HashBiMap.create();
		((Map<ResourceLocation, Object>)slaveset).put(ENTRY_TO_ID, entryToId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onClear(IForgeRegistry<E> is, Map<ResourceLocation, ?> slaveset) {
		((Map<T, E>)slaveset.get(OBJECT_TO_ENTRY)).clear();
		((BiMap<E, Integer>)slaveset.get(ENTRY_TO_ID)).clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onAdd(E obj, int id, Map<ResourceLocation, ?> slaveset) {
		((Map<T, E>)slaveset.get(OBJECT_TO_ENTRY)).put(getWrappedObject(obj), obj);
		((BiMap<E, Integer>)slaveset.get(ENTRY_TO_ID)).put(obj, id);
	}

	@SuppressWarnings("unchecked")
	public static <T, E extends IForgeRegistryEntry<E>> Map<T, E> getObjectToEntryMap(IForgeRegistry<E> registry) {
		return registry.getSlaveMap(OBJECT_TO_ENTRY, Map.class);
	}

	@SuppressWarnings("unchecked")
	public static <E extends IForgeRegistryEntry<E>> BiMap<E, Integer> getEntryIdMap(IForgeRegistry<E> registry) {
		return registry.getSlaveMap(ENTRY_TO_ID, BiMap.class);
	}

	public static <T, E extends IForgeRegistryEntry<E>> Integer mapObjectToId(IForgeRegistry<E> registry, T object) {
		final Map<T, E> objectToEntryMap = CommonRegistryCallbacks.getObjectToEntryMap(registry);
		final E entry = objectToEntryMap.get(object);

		final BiMap<E, Integer> entryIdMap = CommonRegistryCallbacks.getEntryIdMap(registry);
		return entryIdMap.get(entry);
	}
}
