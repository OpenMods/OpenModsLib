package openmods.fakeplayer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import openmods.Log;

public class OpenModsFakePlayer extends FakePlayer {

	OpenModsFakePlayer(ServerWorld world, int id) {
		super(world, createProfile(String.format("OpenModsFakethis-%03d", id)));
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

	public ActionResultType rightClick(@Nonnull ItemStack itemStack, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		// mimics PlayerInteractionManager.processRightClickBlock

		if (itemStack.isEmpty()) return ActionResultType.PASS;

		setHeldItem(hand, itemStack);

		final PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(this, hand, pos, facing, new Vec3d(hitX, hitY, hitZ));
		if (event.isCanceled()) return ActionResultType.PASS;

		final Item usedItem = itemStack.getItem();

		final ActionResultType firstUseResult = itemStack.onItemUseFirst(this, world, pos, hand, facing, hitX, hitY, hitZ);
		if (firstUseResult != ActionResultType.PASS) return firstUseResult;

		boolean bypass = itemStack.isEmpty() || itemStack.getItem().doesSneakBypassUse(itemStack, world, pos, this);
		ActionResultType result = ActionResultType.PASS;

		if (!isSneaking() || bypass || event.getUseBlock() == Event.Result.ALLOW) {
			BlockState iblockstate = world.getBlockState(pos);
			if (event.getUseBlock() != Event.Result.DENY) {
				try {
					if (iblockstate.getBlock().onBlockActivated(world, pos, iblockstate, this, hand, facing, hitX, hitY, hitZ)) {
						result = ActionResultType.SUCCESS;
					}
				} catch (Throwable t) {
					Log.warn(t, "Invalid use of fake player on block %s @ (%s), aborting. Don't do it again", iblockstate, pos);
				}
			}
		}

		if ((result != ActionResultType.SUCCESS && event.getUseItem() != Event.Result.DENY)
				|| (result == ActionResultType.SUCCESS && event.getUseItem() == Event.Result.ALLOW)) {
			try {
				return itemStack.onItemUse(this, world, pos, hand, facing, hitX, hitY, hitZ);
			} catch (Throwable t) {
				Log.warn(t, "Invalid use of fake player with item %s @ (%s), aborting. Don't do it again", usedItem, pos);
				return ActionResultType.PASS;
			}
		}
		return result;
	}
}
