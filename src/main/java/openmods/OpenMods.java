package openmods;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import openmods.config.properties.CommandConfig;
import openmods.config.properties.ConfigProcessing;
import openmods.entity.DelayedEntityLoadManager;
import openmods.fakeplayer.FakePlayerPool;
import openmods.integration.Integration;
import openmods.integration.modules.BuildCraftPipes;
import openmods.network.IdSyncManager;
import openmods.network.event.NetworkEventManager;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.EntityRpcTarget;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.proxy.IOpenModsProxy;
import openmods.source.ClassSourceCollector;
import openmods.source.CommandSource;
import openmods.sync.SyncChannelHolder;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;
import openmods.world.DelayedActionTickHandler;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;

@Mod(modid = "OpenMods", name = "OpenMods", version = "$LIB-VERSION$", dependencies = "required-after:OpenModsCore")
public class OpenMods {

	@Instance(value = "OpenMods")
	public static OpenMods instance;

	@SidedProxy(clientSide = "openmods.proxy.OpenClientProxy", serverSide = "openmods.proxy.OpenServerProxy")
	public static IOpenModsProxy proxy;

	private ClassSourceCollector collector;

	public ClassSourceCollector getCollector() {
		return collector;
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		SyncChannelHolder.ensureLoaded();

		RpcCallDispatcher.INSTANCE.startRegistration()
				.registerInterface(IRpcDirectionBitMap.class)
				.registerInterface(IRpcIntBitMap.class)
				.registerTargetWrapper(EntityRpcTarget.class)
				.registerTargetWrapper(TileEntityRpcTarget.class)
				.registerTargetWrapper(SyncRpcTarget.SyncEntityRpcTarget.class)
				.registerTargetWrapper(SyncRpcTarget.SyncTileEntityRpcTarget.class);

		final File configFile = evt.getSuggestedConfigurationFile();
		Configuration config = new Configuration(configFile);
		ConfigProcessing.processAnnotations(configFile, "OpenMods", config, LibConfig.class);
		if (config.hasChanged()) config.save();

		MinecraftForge.EVENT_BUS.register(DelayedEntityLoadManager.instance);

		MinecraftForge.EVENT_BUS.register(FakePlayerPool.instance);

		FMLCommonHandler.instance().bus().register(DelayedActionTickHandler.INSTANCE);

		collector = new ClassSourceCollector(evt.getAsmData());

		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		Integration.addModule(new BuildCraftPipes());
		Integration.loadModules();
		proxy.postInit();

		NetworkEventManager.INSTANCE.finalizeRegistration();
		RpcCallDispatcher.INSTANCE.finishRegistration();

		// must be after all builders are done
		IdSyncManager.INSTANCE.finishLoading();
	}

	@EventHandler
	public void severStart(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandConfig("om_config_s", true));
		evt.registerServerCommand(new CommandSource("om_source_s", true, collector));
	}
}
