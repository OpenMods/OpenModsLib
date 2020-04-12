package openmods.block;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import openmods.api.ISelectionAware;

public class BlockSelectionHandler {

	@SubscribeEvent
	public void onHighlightDraw(DrawHighlightEvent evt) {
		final RayTraceResult mop = evt.getTarget();

		if (mop != null && mop.getType() == RayTraceResult.Type.BLOCK) {
			final BlockRayTraceResult bop = (BlockRayTraceResult)mop;
			final World world = Minecraft.getInstance().world;
			final BlockPos blockPos = bop.getPos();
			final Block block = world.getBlockState(blockPos).getBlock();

			if (block instanceof ISelectionAware) {
				final boolean result = ((ISelectionAware)block).onSelected(world, blockPos, evt);
				evt.setCanceled(result);
			}
		}
	}

}
