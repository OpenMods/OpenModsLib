package openmods.fakeplayer;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.utils.InventoryUtils;
import openmods.utils.MathUtils;

public class UseItemAction implements PlayerUserReturning<ItemStack> {

	private final ItemStack stack;

	private final Vec3d playerPos;
	private final Vec3d clickPos;
	private final Vec3d hitPos;
	private final EnumFacing side;
	private final EnumHand hand;

	public UseItemAction(ItemStack stack, Vec3d playerPos, Vec3d clickPos, Vec3d hitPos, EnumFacing side, EnumHand hand) {
		super();
		this.stack = stack;
		this.playerPos = playerPos;
		this.clickPos = clickPos;
		this.hitPos = hitPos;
		this.side = side;
		this.hand = hand;
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
				hand,
				side,
				(float)hitPos.xCoord, (float)hitPos.yCoord, (float)hitPos.zCoord);

		return InventoryUtils.returnItem(player.inventory.getCurrentItem());
	}
}
