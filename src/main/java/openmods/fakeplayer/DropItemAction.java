package openmods.fakeplayer;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import openmods.Log;
import openmods.fakeplayer.FakePlayerPool.PlayerUser;

import com.google.common.base.Preconditions;

public class DropItemAction implements PlayerUser {

	private final float x;
	private final float y;
	private final float z;

	private final EnumFacing side;

	private final ItemStack stack;

	public DropItemAction(ItemStack stack, float x, float y, float z, EnumFacing side) {
		this.stack = stack;
		this.x = x;
		this.y = y;
		this.z = z;
		this.side = side;
	}

	@Override
	public void usePlayer(OpenModsFakePlayer player) {
		Preconditions.checkArgument(side == EnumFacing.DOWN, "Other directions than down are not implemented");
		player.setPositionAndRotation(x + 0.5F, y - 1.5, z + 0.5F, 0, 90);

		EntityItem entityItem = player.dropPlayerItemWithRandomChoice(stack.copy(), false);
		if (entityItem != null) {
			entityItem.motionX = 0;
			entityItem.motionY = 0;
			entityItem.motionZ = 0;
		} else {
			Log.info("Item %s drop from this %s aborted by event", stack, this);
		}
	}

}
