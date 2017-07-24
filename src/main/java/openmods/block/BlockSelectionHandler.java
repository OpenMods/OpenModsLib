package openmods.block;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openmods.api.ISelectionAware;

public class BlockSelectionHandler {

	@SubscribeEvent
	public void onHighlightDraw(DrawBlockHighlightEvent evt) {
		final RayTraceResult mop = evt.getTarget();

		if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK) {

			final World world = evt.getPlayer().world;
			final BlockPos blockPos = mop.getBlockPos();
			final Block block = world.getBlockState(blockPos).getBlock();

			if (block instanceof ISelectionAware) {
				final boolean result = ((ISelectionAware)block).onSelected(world, blockPos, evt);
				evt.setCanceled(result);
			}
		}
	}

}
