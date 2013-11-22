package openmods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.ServerListenThread;
import net.minecraft.server.ThreadMinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import openmods.interfaces.IOpenModsProxy;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.Player;

public class OpenClientProxy implements IOpenModsProxy {

	@Override
	public boolean isServerOnly() {
		return false;
	}

	@Override
	public boolean isServerThread() {
		Thread thr = Thread.currentThread();
		return thr instanceof ThreadMinecraftServer
				|| thr instanceof ServerListenThread;
	}

	@Override
	public EntityPlayer getThePlayer() {
		return FMLClientHandler.instance().getClient().thePlayer;
	}

	@Override
	public long getTicks(World worldObj) {
		return worldObj.getTotalWorldTime(); //until fixed //ClientTickHandler.getTicks();
	}

	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().theWorld;
	}

	@Override
	public World getServerWorld(int id) {
		return DimensionManager.getWorld(id);
	}

	@Override
	public void sendPacketToPlayer(Player player, Packet packet) {
		if (player instanceof EntityPlayerMP) ((EntityPlayerMP)player).playerNetServerHandler.sendPacketToPlayer(packet);
		else if (player instanceof EntityClientPlayerMP) ((EntityClientPlayerMP)player).sendQueue.addToSendQueue(packet);
		else throw new UnsupportedOperationException("HOW DO I PACKET?");
	}

	@Override
	public void sendPacketToServer(Packet packet) {
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(packet);
	}

}
