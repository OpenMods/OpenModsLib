package openmods.utils.render;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import openmods.Mods;

import com.google.common.collect.Sets;

public class PaintUtils {

	private final Set<Block> allowed = Sets.newHashSet();

	public static final PaintUtils instance = new PaintUtils();

	protected PaintUtils() {
		allowed.add(Blocks.stone);
		allowed.add(Blocks.cobblestone);
		allowed.add(Blocks.mossy_cobblestone);
		allowed.add(Blocks.sandstone);
		allowed.add(Blocks.iron_block);
		allowed.add(Blocks.stonebrick);
		allowed.add(Blocks.glass);
		allowed.add(Blocks.planks);
		allowed.add(Blocks.dirt);
		allowed.add(Blocks.log);
		allowed.add(Blocks.log2);
		allowed.add(Blocks.gold_block);
		allowed.add(Blocks.emerald_block);
		allowed.add(Blocks.lapis_block);
		allowed.add(Blocks.quartz_block);
		allowed.add(Blocks.end_stone);
		if (Loader.isModLoaded(Mods.TINKERSCONSTRUCT)) {
			addBlocksForMod(Mods.TINKERSCONSTRUCT,
					"GlassBlock",
					"decoration.multibrick",
					"decoration.multibrickfancy");
		}
		if (Loader.isModLoaded(Mods.EXTRAUTILITIES)) {
			addBlocksForMod(Mods.EXTRAUTILITIES,
					"greenScreen",
					"extrautils:decor");
		}
		if (Loader.isModLoaded(Mods.BIOMESOPLENTY)) {
			addBlocksForMod(Mods.BIOMESOPLENTY,
					"bop.planks");
		}
	}

	protected void addBlocksForMod(String modId, String... blocks) {
		for (String blockName : blocks) {
			Block block = GameRegistry.findBlock(modId, blockName);
			if (block != null) allowed.add(block);
		}
	}

	public boolean isAllowedToReplace(Block block) {
		if (block == null || block.canProvidePower()) return false;
		return allowed.contains(block);
	}

	public boolean isAllowedToReplace(World world, BlockPos pos) {
		if (world.isAirBlock(pos)) { return false; }
		return isAllowedToReplace(world.getBlockState(pos).getBlock());
	}
}
