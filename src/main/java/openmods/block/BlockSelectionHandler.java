package openmods.block;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import openmods.api.ISelectionAware;

public class BlockSelectionHandler {

	@SubscribeEvent
	public void onHighlightDraw(DrawBlockHighlightEvent evt) {
		final MovingObjectPosition mop = evt.target;

		if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
			final int x = mop.blockX;
			final int y = mop.blockY;
			final int z = mop.blockZ;

			final World world = evt.player.worldObj;
			final Block block = world.getBlock(x, y, z);

			if (block instanceof ISelectionAware) {
				final boolean result = ((ISelectionAware)block).onSelected(world, x, y, z, evt);
				evt.setCanceled(result);
			}
		}
	}

}
