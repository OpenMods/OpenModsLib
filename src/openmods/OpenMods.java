package openmods;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import openmods.common.api.IOpenMod;
import openmods.interfaces.IProxy;
import openmods.network.PacketHandler;
import openmods.sync.SyncableManager;

@Mod(modid = "OpenMods", name = "OpenMods", version = "@VERSION@")
@NetworkMod(serverSideRequired = true, clientSideRequired = false, channels = { PacketHandler.CHANNEL_SYNC, PacketHandler.CHANNEL_EVENTS }, packetHandler = PacketHandler.class)
public class OpenMods {

	@Instance(value = "OpenMods")
	public static OpenMods instance;
	
	@SidedProxy(clientSide = "openmods.client.OpenClientProxy", serverSide = "openmods.common.OpenServerProxy")
	public static IProxy proxy;

	public static SyncableManager syncableManager;
	
	@EventHandler
	public void init(FMLInitializationEvent evt) {
		syncableManager = new SyncableManager();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
	}
}
