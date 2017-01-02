package openmods.utils;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.GameData;

public class ModIdentifier {

	public static final ModIdentifier INSTANCE = new ModIdentifier();

	private final Map<Item, ModContainer> itemCache = Maps.newHashMap();

	private final Map<Block, ModContainer> blockCache = Maps.newHashMap();

	public ModContainer getModItemStack(ItemStack stack) {
		if (stack == null) return null;
		Item item = stack.getItem();
		if (item == null) return null;

		return getModForItem(item);
	}

	public ModContainer getModForItem(Item item) {
		if (itemCache.containsKey(item)) return itemCache.get(item);

		ModContainer result = identifyItem(item);
		itemCache.put(item, result);
		return result;
	}

	public ModContainer getModForBlock(Block block) {
		if (blockCache.containsKey(block)) return blockCache.get(block);

		ModContainer result = identifyBlock(block);
		blockCache.put(block, result);
		return result;
	}

	private static ModContainer identifyBlock(Block block) {
		final ResourceLocation id = GameData.getBlockRegistry().getNameForObject(block);
		return findModContainer(id);
	}

	private static ModContainer identifyItem(Item item) {
		final ResourceLocation id = GameData.getItemRegistry().getNameForObject(item);
		return findModContainer(id);
	}

	private static ModContainer findModContainer(ResourceLocation id) {
		if (id == null) return null;

		String modId = id.getResourceDomain();
		return Loader.instance().getIndexedModList().get(modId);
	}
}
