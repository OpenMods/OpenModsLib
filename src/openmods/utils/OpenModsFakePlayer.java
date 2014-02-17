package openmods.utils;

import java.util.Map;
import java.util.WeakHashMap;

import openmods.Log;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.FakePlayer;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import com.google.common.base.Preconditions;

//TODO: Discuss if we make seperate players for seperate mods
public class OpenModsFakePlayer extends FakePlayer {
	private static final Map<World, OpenModsFakePlayer> PLAYERS = new WeakHashMap<World, OpenModsFakePlayer>();

	public static OpenModsFakePlayer getPlayerForWorld(World world) {
		OpenModsFakePlayer player = PLAYERS.get(world);
		if (player == null) {
			player = new OpenModsFakePlayer(world);
			PLAYERS.put(world, player);
		}
		return player;
	}

	private OpenModsFakePlayer(World world) {
		super(world, "OpenModsFakePlayer");
	}

	@Override
	public void setDead() {
		PLAYERS.remove(worldObj.provider.dimensionId);
		super.setDead();
	}

	public ItemStack equipWithAndRightClick(ItemStack itemStack, Vec3 currentPos, Vec3 hitVector, ForgeDirection side, boolean blockExists) {
		setPosition(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord);

		if (blockExists) {

			// find rotations
			float deltaX = (float)(currentPos.xCoord - hitVector.xCoord);
			float deltaY = (float)(currentPos.yCoord - hitVector.yCoord);
			float deltaZ = (float)(currentPos.zCoord - hitVector.zCoord);
			setSneaking(false);
			if (rightClick(
					inventory.getCurrentItem(),
					(int)currentPos.xCoord,
					(int)currentPos.yCoord,
					(int)currentPos.zCoord,
					side,
					deltaX, deltaY, deltaZ,
					blockExists)) {
				setSneaking(true);

				return InventoryUtils.returnItem(inventory.getCurrentItem());
			}
			hitVector.yCoord++;
			setSneaking(true);
		}

		// find rotations
		float deltaX = (float)(currentPos.xCoord - hitVector.xCoord);
		float deltaY = (float)(currentPos.yCoord - hitVector.yCoord);
		float deltaZ = (float)(currentPos.zCoord - hitVector.zCoord);
		float distanceInGroundPlain = (float)Math.sqrt((float)MathUtils.lengthSq(deltaX, deltaZ));

		float pitch = (float)(Math.atan2(deltaZ, deltaX) * 180 / Math.PI);
		float hue = (float)(Math.atan2(deltaY, distanceInGroundPlain) * 180 / Math.PI);

		setRotation(pitch, hue);

		inventory.clearInventory(-1, -1);
		inventory.addItemStackToInventory(itemStack);
		rightClick(
				inventory.getCurrentItem(),
				(int)currentPos.xCoord,
				(int)currentPos.yCoord,
				(int)currentPos.zCoord,
				side,
				deltaX, deltaY, deltaZ,
				blockExists);

		return InventoryUtils.returnItem(inventory.getCurrentItem());
	}

	public void dropItemAt(ItemStack itemStack, int x, int y, int z, ForgeDirection direction) {
		setPosition(x + 0.5F, y - 1.5, z + 0.5F);
		Preconditions.checkArgument(direction == ForgeDirection.DOWN, "Other directions than down is not implemented");
		setRotation(0, 90);
		EntityItem entityItem = dropPlayerItem(itemStack);
		entityItem.motionX = 0;
		entityItem.motionY = 0;
		entityItem.motionZ = 0;
	}

	private boolean rightClick(ItemStack itemStack, int x, int y, int z, ForgeDirection side, float deltaX, float deltaY, float deltaZ, boolean blockExists) {
		if (itemStack == null) return false;

		final int opposite = side.getOpposite().ordinal();
		PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, x, y, z, opposite);

		if (event.isCanceled()) { return false; }

		final Item usedItem = itemStack.getItem();

		if (usedItem.onItemUseFirst(itemStack, this, worldObj, x, y, z, opposite, deltaX, deltaY, deltaZ)) { return true; }

		if (event.useBlock != Event.Result.DENY && (isSneaking() || usedItem.shouldPassSneakingClickToBlock(worldObj, x, y, z))) {
			int blockId = worldObj.getBlockId(x, y, z);
			Block block = Block.blocksList[blockId];
			if (block != null) try {
				if (block.onBlockActivated(worldObj, x, y, z, this, opposite, deltaX, deltaY, deltaZ)) return true;
			} catch (Throwable t) {
				Log.warn(t, "Invalid use of fake player on block %s @ (%d,%d,%d), aborting. Don't do it again", block, x, y, z);
			}
		}

		if (event.useItem == Event.Result.DENY || usedItem instanceof ItemBlock && blockExists) return false;
		try {
			return itemStack.tryPlaceItemIntoWorld(this, worldObj, x, y, z, opposite, deltaX, deltaY, deltaZ);
		} catch (Throwable t) {
			Log.warn(t, "Invalid use of fake player with item %s @ (%d,%d,%d), aborting. Don't do it again", usedItem, x, y, z);
			return false;
		}
	}
}
