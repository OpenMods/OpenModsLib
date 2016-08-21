package openmods.network.targets;

import com.google.common.base.Preconditions;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import java.util.Collection;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import openmods.network.IPacketTargetSelector;
import openmods.utils.NetUtils;

public class SelectEntityWatchers implements IPacketTargetSelector<Entity> {

	public static final IPacketTargetSelector<Entity> INSTANCE = new SelectEntityWatchers();

	@Override
	public boolean isAllowedOnSide(Side side) {
		return side == Side.SERVER;
	}

	@Override
	public void listDispatchers(Entity entity, Collection<NetworkDispatcher> result) {
		Preconditions.checkArgument(entity.worldObj instanceof WorldServer, "Invalid side");
		WorldServer server = (WorldServer)entity.worldObj;
		Set<EntityPlayerMP> players = NetUtils.getPlayersWatchingEntity(server, entity.getEntityId());

		for (EntityPlayerMP player : players) {
			NetworkDispatcher dispatcher = NetUtils.getPlayerDispatcher(player);
			result.add(dispatcher);
		}
	}

	@Override
	public Entity castArg(Object arg) {
		return (Entity)arg;
	}

}
