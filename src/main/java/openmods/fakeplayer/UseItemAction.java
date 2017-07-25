package openmods.fakeplayer;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.utils.InventoryUtils;

public class UseItemAction implements PlayerUserReturning<ItemStack> {

	@Nonnull
	private final ItemStack stack;

	private final Vec3d playerPos;
	private final Vec3d clickPos;
	private final Vec3d hitPos;
	private final EnumFacing side;
	private final EnumHand hand;

	private final float yaw;
	private final float pitch;

	public UseItemAction(ItemStack stack, Vec3d playerPos, Vec3d clickPos, Vec3d hitPos, EnumFacing side, EnumHand hand) {
		super();
		this.stack = stack;
		this.playerPos = playerPos;
		this.clickPos = clickPos;
		this.hitPos = hitPos;
		this.side = side;
		this.hand = hand;

		final float deltaX = (float)(clickPos.xCoord - playerPos.xCoord);
		final float deltaY = (float)(clickPos.yCoord - playerPos.yCoord);
		final float deltaZ = (float)(clickPos.zCoord - playerPos.zCoord);

		this.pitch = -(float)Math.toDegrees(Math.asin(deltaY));
		this.yaw = -(float)Math.toDegrees(Math.atan2(deltaX, deltaZ));
	}

	@Override
	@Nonnull
	public ItemStack usePlayer(OpenModsFakePlayer player) {
		player.inventory.currentItem = 0;
		player.inventory.setInventorySlotContents(0, stack);

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
