package openmods.utils;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

public class ModIdentifier {

	public static final ModIdentifier INSTANCE = new ModIdentifier();

	private final Map<Item, ModContainer> itemCache = Maps.newHashMap();

	private final Map<Block, ModContainer> blockCache = Maps.newHashMap();

	public ModContainer getModItemStack(@Nonnull ItemStack stack) {
		if (stack.isEmpty()) return null;
		return getModForItem(stack.getItem());
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
		final ResourceLocation id = Block.REGISTRY.getNameForObject(block);
		return findModContainer(id);
	}

	private static ModContainer identifyItem(Item item) {
		final ResourceLocation id = Item.REGISTRY.getNameForObject(item);
		return findModContainer(id);
	}

	private static ModContainer findModContainer(ResourceLocation id) {
		if (id == null) return null;

		String modId = id.getResourceDomain();
		return Loader.instance().getIndexedModList().get(modId);
	}
}
