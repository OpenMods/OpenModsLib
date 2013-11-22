package openmods.interfaces;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.Player;

public interface IOpenModsProxy {

	public boolean isServerOnly();

	public boolean isServerThread();

	public World getClientWorld();

	public World getServerWorld(int dimension);

	public EntityPlayer getThePlayer();

	public long getTicks(World worldObj);

	public void sendPacketToPlayer(Player player, Packet packet);

	public void sendPacketToServer(Packet packet);
}