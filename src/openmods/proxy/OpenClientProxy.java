package openmods.proxy;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.ServerListenThread;
import net.minecraft.server.ThreadMinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
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
	public boolean isClientPlayer(Entity player) {
		return player instanceof EntityPlayerSP;
	}

	@Override
	public long getTicks(World worldObj) {
		if (worldObj != null) { return worldObj.getTotalWorldTime(); }
		World cWorld = getClientWorld();
		if (cWorld != null) return cWorld.getTotalWorldTime();
		return 0;
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

	@Override
	public File getMinecraftDir() {
		return Minecraft.getMinecraft().mcDataDir;
	}

	@Override
	public String getLogFileName() {
		return "ForgeModLoader-client-0.log";
	}

}
