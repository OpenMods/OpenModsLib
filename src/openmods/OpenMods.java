package openmods;

import openmods.interfaces.IOpenModsProxy;
import openmods.network.EventPacket;
import openmods.network.PacketHandler;
import openmods.sync.SyncableManager;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = "OpenMods", name = "OpenMods", version = "@VERSION@")
@NetworkMod(serverSideRequired = true, clientSideRequired = false, channels = { PacketHandler.CHANNEL_SYNC, PacketHandler.CHANNEL_EVENTS }, packetHandler = PacketHandler.class)
public class OpenMods {

	@Instance(value = "OpenMods")
	public static OpenMods instance;

	@SidedProxy(clientSide = "openmods.OpenClientProxy", serverSide = "openmods.OpenServerProxy")
	public static IOpenModsProxy proxy;

	public static SyncableManager syncableManager;

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		Log.logger = evt.getModLog();
		EventPacket.regiterCorePackets();
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		syncableManager = new SyncableManager();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {}
}
