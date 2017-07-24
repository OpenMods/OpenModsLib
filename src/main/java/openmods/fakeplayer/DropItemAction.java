package openmods.fakeplayer;

import javax.vecmath.Vector3f;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import openmods.Log;
import openmods.fakeplayer.FakePlayerPool.PlayerUser;

public class DropItemAction implements PlayerUser {

	private final double x;
	private final double y;
	private final double z;

	private final Vector3f v;

	private float yaw;
	private float pitch;

	private final ItemStack stack;

	public DropItemAction(ItemStack stack, double x, double y, double z, float vx, float vy, float vz) {
		this.stack = stack;
		this.x = x;
		this.y = y;
		this.z = z;
		this.v = new Vector3f(vx, vy, vz);

		final Vector3f nv = new Vector3f();
		nv.normalize(this.v);

		this.pitch = -(float)Math.toDegrees(Math.asin(nv.y));
		this.yaw = -(float)Math.toDegrees(Math.atan2(nv.x, nv.z));
	}

	public DropItemAction(ItemStack stack, BlockPos pos, float vx, float vy, float vz) {
		this(stack, pos.getX(), pos.getY(), pos.getZ(), vx, vy, vz);
	}

	@Override
	public void usePlayer(OpenModsFakePlayer player) {
		player.setPositionAndRotation(x, y - player.getEyeHeight(), z, yaw, pitch);

		final EntityItem itemToDrop = new EntityItem(player.getEntityWorld(), x, y, z, stack.copy());
		itemToDrop.setPosition(itemToDrop.posX, itemToDrop.posY - itemToDrop.height, itemToDrop.posZ);
		itemToDrop.setPickupDelay(40);

		ItemTossEvent event = new ItemTossEvent(itemToDrop, player);
		if (MinecraftForge.EVENT_BUS.post(event)) {
			Log.info("Item %s drop from this %s aborted by event", stack, this);
		} else {
			final EntityItem droppedItem = event.getEntityItem();

			droppedItem.motionX = v.x;
			droppedItem.motionY = v.y;
			droppedItem.motionZ = v.z;

			player.getEntityWorld().spawnEntity(droppedItem);
		}
	}

}
