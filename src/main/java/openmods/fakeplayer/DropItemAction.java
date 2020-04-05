package openmods.fakeplayer;

import javax.annotation.Nonnull;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import openmods.Log;
import openmods.fakeplayer.FakePlayerPool.PlayerUser;

public class DropItemAction implements PlayerUser {

	private final double x;
	private final double y;
	private final double z;

	private final Vec3d v;

	private final float yaw;
	private final float pitch;

	@Nonnull
	private final ItemStack stack;

	public DropItemAction(@Nonnull ItemStack stack, double x, double y, double z, float vx, float vy, float vz) {
		this.stack = stack;
		this.x = x;
		this.y = y;
		this.z = z;
		this.v = new Vec3d(vx, vy, vz);

		final Vec3d nv = v.normalize();

		this.pitch = -(float)Math.toDegrees(Math.asin(nv.y));
		this.yaw = -(float)Math.toDegrees(Math.atan2(nv.x, nv.z));
	}

	public DropItemAction(@Nonnull ItemStack stack, BlockPos pos, float vx, float vy, float vz) {
		this(stack, pos.getX(), pos.getY(), pos.getZ(), vx, vy, vz);
	}

	@Override
	public void usePlayer(OpenModsFakePlayer player) {
		player.setPositionAndRotation(x, y - player.getEyeHeight(), z, yaw, pitch);

		// TODO 1.14 Check dropper for changes
		final ItemEntity itemToDrop = new ItemEntity(player.getEntityWorld(), x, y, z, stack.copy());
		itemToDrop.setPosition(itemToDrop.posX, itemToDrop.posY - itemToDrop.getHeight(), itemToDrop.posZ);
		itemToDrop.setPickupDelay(40);

		ItemTossEvent event = new ItemTossEvent(itemToDrop, player);
		if (MinecraftForge.EVENT_BUS.post(event)) {
			Log.info("Item %s drop from this %s aborted by event", stack, this);
		} else {
			final ItemEntity droppedItem = event.getEntityItem();

			droppedItem.setMotion(v);

			player.getEntityWorld().addEntity(droppedItem);
		}
	}

}
