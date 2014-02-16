package openmods;

import java.io.File;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import openmods.config.CommandConfig;
import openmods.config.ConfigProcessing;
import openmods.entity.DelayedEntityLoadManager;
import openmods.integration.Integration;
import openmods.network.EventPacket;
import openmods.network.PacketHandler;
import openmods.network.events.TileEntityEventHandler;
import openmods.proxy.IOpenModsProxy;
import openmods.sync.SyncableManager;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = "OpenMods", name = "OpenMods", version = "0.2", dependencies = "required-after:OpenModsCore")
@NetworkMod(serverSideRequired = true, clientSideRequired = false, channels = { PacketHandler.CHANNEL_SYNC, PacketHandler.CHANNEL_EVENTS }, packetHandler = PacketHandler.class)
public class OpenMods {

	@Instance(value = "OpenMods")
	public static OpenMods instance;

	@SidedProxy(clientSide = "openmods.proxy.OpenClientProxy", serverSide = "openmods.proxy.OpenServerProxy")
	public static IOpenModsProxy proxy;

	public static SyncableManager syncableManager;

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		Log.logger = evt.getModLog();
		EventPacket.registerCorePackets();

		final File configFile = evt.getSuggestedConfigurationFile();
		Configuration config = new Configuration(configFile);
		ConfigProcessing.processAnnotations(configFile, "OpenMods", config, LibConfig.class);
		if (config.hasChanged()) config.save();

		MinecraftForge.EVENT_BUS.register(new TileEntityEventHandler());

		MinecraftForge.EVENT_BUS.register(DelayedEntityLoadManager.instance);

		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		syncableManager = new SyncableManager();
		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		Integration.selectModules();
		proxy.postInit();
	}

	@EventHandler
	public void severStart(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandConfig("om_config_s", true));
	}
}
