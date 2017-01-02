package openmods.utils.render;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import openmods.Mods;

public class PaintUtils {

	private final Set<Block> allowed = Sets.newHashSet();

	public static final PaintUtils instance = new PaintUtils();

	protected PaintUtils() {
		allowed.add(Blocks.STONE);
		allowed.add(Blocks.COBBLESTONE);
		allowed.add(Blocks.MOSSY_COBBLESTONE);
		allowed.add(Blocks.SANDSTONE);
		allowed.add(Blocks.IRON_BLOCK);
		allowed.add(Blocks.STONEBRICK);
		allowed.add(Blocks.GLASS);
		allowed.add(Blocks.PLANKS);
		allowed.add(Blocks.DIRT);
		allowed.add(Blocks.LOG);
		allowed.add(Blocks.LOG2);
		allowed.add(Blocks.GOLD_BLOCK);
		allowed.add(Blocks.EMERALD_BLOCK);
		allowed.add(Blocks.LAPIS_BLOCK);
		allowed.add(Blocks.QUARTZ_BLOCK);
		allowed.add(Blocks.END_STONE);
		// TODO more blocks
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
			Block block = Block.REGISTRY.getObject(new ResourceLocation(modId, blockName));
			if (block != null) allowed.add(block);
		}
	}

	public boolean isAllowedToReplace(IBlockState block) {
		if (block.canProvidePower()) return false;
		return allowed.contains(block.getBlock());
	}

	public boolean isAllowedToReplace(World world, BlockPos pos) {
		if (world.isAirBlock(pos)) { return false; }
		return isAllowedToReplace(world.getBlockState(pos));
	}
}
