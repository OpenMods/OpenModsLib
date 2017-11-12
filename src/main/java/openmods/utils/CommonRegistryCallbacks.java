package openmods.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryManager;
import openmods.OpenMods;
import openmods.network.rpc.MethodEntry;

public abstract class CommonRegistryCallbacks<T, E extends IForgeRegistryEntry<E>> implements IForgeRegistry.AddCallback<E>, IForgeRegistry.ClearCallback<E>, IForgeRegistry.CreateCallback<E> {

	private static final ResourceLocation OBJECT_TO_ENTRY = OpenMods.location("object_to_entry");

	private static final ResourceLocation ENTRY_TO_ID = OpenMods.location("entry_to_id");

	protected abstract T getWrappedObject(E entry);

	@Override
	public void onCreate(IForgeRegistryInternal<E> owner, RegistryManager stage) {
		final Map<T, E> classToEntryMap = Maps.newHashMap();
		owner.setSlaveMap(OBJECT_TO_ENTRY, classToEntryMap);

		final BiMap<MethodEntry, Integer> entryToId = HashBiMap.create();
		owner.setSlaveMap(ENTRY_TO_ID, entryToId);
	}

	@Override
	public void onClear(IForgeRegistryInternal<E> owner, RegistryManager stage) {
		getObjectToEntryMap(owner).clear();
		getEntryIdMap(owner).clear();
	}

	@Override
	public void onAdd(IForgeRegistryInternal<E> owner, RegistryManager stage, int id, E obj, @Nullable E oldObj) {
		getObjectToEntryMap(owner).put(getWrappedObject(obj), obj);
		getEntryIdMap(owner).put(obj, id);
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
