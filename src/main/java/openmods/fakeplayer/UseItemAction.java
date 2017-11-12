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

		final float deltaX = (float)(clickPos.x - playerPos.x);
		final float deltaY = (float)(clickPos.y - playerPos.y);
		final float deltaZ = (float)(clickPos.z - playerPos.z);

		this.pitch = -(float)Math.toDegrees(Math.asin(deltaY));
		this.yaw = -(float)Math.toDegrees(Math.atan2(deltaX, deltaZ));
	}

	@Override
	@Nonnull
	public ItemStack usePlayer(OpenModsFakePlayer player) {
		player.inventory.currentItem = 0;
		player.inventory.setInventorySlotContents(0, stack);

		player.setPositionAndRotation(playerPos.x, playerPos.y, playerPos.z, yaw, pitch);

		player.rightClick(
				stack,
				new BlockPos(clickPos.x, clickPos.y, clickPos.z),
				hand,
				side,
				(float)hitPos.x, (float)hitPos.y, (float)hitPos.z);

		return InventoryUtils.returnItem(player.inventory.getCurrentItem());
	}
}
