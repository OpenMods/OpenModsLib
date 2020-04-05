package openmods.fakeplayer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
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
	public void remove() {
		inventory.clear();
		super.remove();
	}

	public ActionResultType rightClick(@Nonnull ItemStack itemStack, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		// TODO 1.14 re-do
		return ActionResultType.PASS;
	}
}
