package openmods.fakeplayer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import openmods.Log;

public class OpenModsFakePlayer extends FakePlayer {

	OpenModsFakePlayer(WorldServer worldObj, int id) {
		super(worldObj, createProfile(String.format("OpenModsFakethis-%03d", id)));
		final GameProfile profile = getGameProfile();
		Log.debug("Creating new fake this: name = %s, id = %s", profile.getName(), profile.getId());
	}

	private static GameProfile createProfile(String name) {
		UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());
		return new GameProfile(uuid, name);
	}

	@Override
	public void setDead() {
		inventory.clear();
		isDead = true;
	}

	public boolean rightClick(ItemStack itemStack, BlockPos pos, EnumFacing face, float hitX, float hitY, float hitZ) {
		// mimics ItemInworldObjManager.activateBlockOrUseItem

		if (itemStack == null) return false;

		PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(this, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, worldObj, pos, face, new Vec3(hitX, hitY, hitZ));
		if (event.isCanceled()) return false;

		final IBlockState iblockstate = worldObj.getBlockState(pos);

		final Item usedItem = itemStack.getItem();

		if (!isSneaking() || usedItem.doesSneakBypassUse(worldObj, pos, this)) {
			if (event.useBlock != Result.DENY) {
				final Block block = iblockstate.getBlock();
				try {
					if (block.onBlockActivated(worldObj, pos, iblockstate, this, face, hitX, hitY, hitZ)) return true;
				} catch (Throwable t) {
					Log.warn(t, "Invalid use of fake player on block %s @ (%s), aborting. Don't do it again", block, pos);
				}
			}

		}
		if (event.useItem != net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
			try {
				return itemStack.onItemUse(this, worldObj, pos, face, hitX, hitY, hitZ);
			} catch (Throwable t) {
				Log.warn(t, "Invalid use of fake player with item %s @ (%s), aborting. Don't do it again", usedItem, pos);
				return false;
			}
		}

		return false;
	}
}
