package openmods.fakeplayer;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.eventhandler.Event;
import java.util.UUID;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import openmods.Log;

public class OpenModsFakePlayer extends FakePlayer {

	OpenModsFakePlayer(WorldServer world, int id) {
		super(world, createProfile(String.format("OpenModsFakePlayer-%03d", id)));
		final GameProfile profile = getGameProfile();
		Log.debug("Creating new fake player: name = %s, id = %s", profile.getName(), profile.getId());
	}

	private static GameProfile createProfile(String name) {
		UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());
		return new GameProfile(uuid, name);
	}

	@Override
	public void setDead() {
		inventory.clearInventory(null, -1);
		isDead = true;
	}

	public void dropItemAt(ItemStack itemStack, int x, int y, int z, ForgeDirection direction) {
		setPosition(x + 0.5F, y - 1.5, z + 0.5F);
		Preconditions.checkArgument(direction == ForgeDirection.DOWN, "Other directions than down are not implemented");
		setRotation(0, 90);

		EntityItem entityItem = dropPlayerItemWithRandomChoice(itemStack, false);
		if (entityItem != null) {
			entityItem.motionX = 0;
			entityItem.motionY = 0;
			entityItem.motionZ = 0;
		} else {
			Log.info("Item %s drop from player %s aborted by event", itemStack, this);
		}
	}

	// parts of ItemInWorldManager.activateBlockOrUseItem
	public boolean tryPlaceItem(ItemStack itemStack, int x, int y, int z, ForgeDirection side, float hitX, float hitY, float hitZ) {
		if (itemStack == null) return false;

		final int opposite = side.getOpposite().ordinal();
		PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, x, y, z, opposite, worldObj);

		if (event.isCanceled()) { return false; }

		final Item usedItem = itemStack.getItem();

		if (usedItem.onItemUseFirst(itemStack, this, worldObj, x, y, z, opposite, hitX, hitY, hitZ)) { return true; }

		if (event.useItem == Event.Result.DENY) return false;

		try {
			return itemStack.tryPlaceItemIntoWorld(this, worldObj, x, y, z, opposite, hitX, hitY, hitZ);
		} catch (Throwable t) {
			Log.warn(t, "Invalid use of fake player with item %s @ (%d,%d,%d), aborting. Don't do it again", usedItem, x, y, z);
			return false;
		}
	}
}
