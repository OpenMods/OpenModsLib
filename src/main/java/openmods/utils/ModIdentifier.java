package openmods.utils;

import com.google.common.collect.Maps;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

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
		return findModContainer(GameRegistry.findUniqueIdentifierFor(block));
	}

	private ModContainer identifyItem(Item item) {
		if (item instanceof ItemBlock) return getModForBlock(((ItemBlock)item).field_150939_a);
		return findModContainer(GameRegistry.findUniqueIdentifierFor(item));
	}

	private static ModContainer findModContainer(UniqueIdentifier id) {
		if (id == null) return null;

		String modId = id.modId;
		return Loader.instance().getIndexedModList().get(modId);
	}
}
