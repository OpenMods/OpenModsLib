package openmods.fakeplayer;

import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.utils.InventoryUtils;
import openmods.utils.MathUtils;

public class UseItemAction implements PlayerUserReturning<ItemStack> {

	private final ItemStack stack;
	private final Vec3 pos;
	private final Vec3 hit;
	private final EnumFacing side;

	public UseItemAction(ItemStack stack, Vec3 pos, Vec3 hit, EnumFacing side) {
		this.stack = stack;
		this.pos = pos;
		this.hit = hit;
		this.side = side;
	}

	@Override
	public ItemStack usePlayer(OpenModsFakePlayer player) {
		player.inventory.currentItem = 0;
		player.inventory.setInventorySlotContents(0, stack);

		final float deltaX = (float)(pos.xCoord - hit.xCoord);
		final float deltaY = (float)(pos.yCoord - hit.yCoord);
		final float deltaZ = (float)(pos.zCoord - hit.zCoord);
		final float distanceInGroundPlain = (float)Math.sqrt((float)MathUtils.lengthSq(deltaX, deltaZ));

		final float pitch = (float)(Math.atan2(deltaZ, deltaX) * 180 / Math.PI);
		final float yaw = (float)(Math.atan2(deltaY, distanceInGroundPlain) * 180 / Math.PI);

		player.setPositionAndRotation(pos.xCoord, pos.yCoord, pos.zCoord, yaw, pitch);

		player.rightClick(
				stack,
				new BlockPos(hit),
				side,
				(float)hit.xCoord, (float)hit.yCoord, (float)hit.zCoord);

		return InventoryUtils.returnItem(player.inventory.getCurrentItem());
	}
}
