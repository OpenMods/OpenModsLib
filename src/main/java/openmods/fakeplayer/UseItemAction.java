package openmods.fakeplayer;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.utils.InventoryUtils;

public class UseItemAction implements PlayerUserReturning<ItemStack> {

	@Nonnull
	private final ItemStack stack;

	private final Vector3d playerPos;
	private final Vector3d clickPos;
	private final Vector3d hitPos;
	private final Direction side;
	private final Hand hand;

	private final float yaw;
	private final float pitch;

	public UseItemAction(ItemStack stack, Vector3d playerPos, Vector3d clickPos, Vector3d hitPos, Direction side, Hand hand) {
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
