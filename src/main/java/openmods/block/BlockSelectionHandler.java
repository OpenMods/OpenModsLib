package openmods.block;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openmods.api.ISelectionAware;

public class BlockSelectionHandler {

	@SubscribeEvent
	public void onHighlightDraw(DrawBlockHighlightEvent evt) {
		final MovingObjectPosition mop = evt.target;

		if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {

			final World world = evt.player.worldObj;
			final BlockPos blockPos = mop.getBlockPos();
			final Block block = world.getBlockState(blockPos).getBlock();

			if (block instanceof ISelectionAware) {
				final boolean result = ((ISelectionAware)block).onSelected(world, blockPos, evt);
				evt.setCanceled(result);
			}
		}
	}

}
