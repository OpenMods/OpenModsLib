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

	private final Vec3 playerPos;
	private final Vec3 clickPos;
	private final Vec3 hitPos;
	private final EnumFacing side;

	public UseItemAction(ItemStack stack, Vec3 playerPos, Vec3 clickPos, Vec3 hitPos, EnumFacing side) {
		this.stack = stack;
		this.playerPos = playerPos;
		this.clickPos = clickPos;
		this.hitPos = hitPos;
		this.side = side;
	}

	@Override
	public ItemStack usePlayer(OpenModsFakePlayer player) {
		player.inventory.currentItem = 0;
		player.inventory.setInventorySlotContents(0, stack);

		final float deltaX = (float)(clickPos.xCoord - playerPos.xCoord);
		final float deltaY = (float)(clickPos.yCoord - playerPos.yCoord);
		final float deltaZ = (float)(clickPos.zCoord - playerPos.zCoord);
		final float distanceInGroundPlain = (float)Math.sqrt((float)MathUtils.lengthSq(deltaX, deltaZ));

		final float yaw = (float)(Math.atan2(deltaX, deltaZ) * -180 / Math.PI);
		final float pitch = (float)(Math.atan2(deltaY, distanceInGroundPlain) * -180 / Math.PI);

		player.setPositionAndRotation(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord, yaw, pitch);

		player.rightClick(
				stack,
				new BlockPos(clickPos.xCoord, clickPos.yCoord, clickPos.zCoord),
				side,
				(float)hitPos.xCoord, (float)hitPos.yCoord, (float)hitPos.zCoord);

		return InventoryUtils.returnItem(player.inventory.getCurrentItem());
	}
}
