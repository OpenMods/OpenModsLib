package openmods;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import java.io.File;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import openmods.block.BlockDropsStore;
import openmods.config.ConfigChangeListener;
import openmods.config.ConfigStorage;
import openmods.config.properties.CommandConfig;
import openmods.config.properties.ConfigProcessing;
import openmods.entity.DelayedEntityLoadManager;
import openmods.entity.EntityBlock;
import openmods.fakeplayer.FakePlayerPool;
import openmods.integration.Integration;
import openmods.integration.modules.BuildCraftPipes;
import openmods.liquids.BucketFillHandler;
import openmods.network.IdSyncManager;
import openmods.network.event.NetworkEventManager;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.EntityRpcTarget;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.proxy.IOpenModsProxy;
import openmods.reflection.TypeVariableHolderHandler;
import openmods.source.ClassSourceCollector;
import openmods.source.CommandSource;
import openmods.sync.SyncChannelHolder;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;
import openmods.world.DelayedActionTickHandler;
import openmods.world.DropCapture;

@Mod(modid = OpenMods.MODID, name = OpenMods.MODID, version = "$LIB-VERSION$", dependencies = "required-after:OpenModsCore", guiFactory = "openmods.GuiFactory")
public class OpenMods {

	public static final String MODID = "OpenMods";

	private static final int ENTITY_BLOCK_ID = 804;

	@Instance(MODID)
	public static OpenMods instance;

	@SidedProxy(clientSide = "openmods.proxy.OpenClientProxy", serverSide = "openmods.proxy.OpenServerProxy")
	public static IOpenModsProxy proxy;

	private ClassSourceCollector collector;

	public ClassSourceCollector getCollector() {
		return collector;
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		new TypeVariableHolderHandler().fillAllHolders(evt.getAsmData());
		SyncChannelHolder.ensureLoaded();

		NetworkEventManager.INSTANCE.startRegistration();

		RpcCallDispatcher.INSTANCE.startRegistration()
				.registerInterface(IRpcDirectionBitMap.class)
				.registerInterface(IRpcIntBitMap.class)
				.registerTargetWrapper(EntityRpcTarget.class)
				.registerTargetWrapper(TileEntityRpcTarget.class)
				.registerTargetWrapper(SyncRpcTarget.SyncEntityRpcTarget.class)
				.registerTargetWrapper(SyncRpcTarget.SyncTileEntityRpcTarget.class);

		final File configFile = evt.getSuggestedConfigurationFile();
		Configuration config = new Configuration(configFile);
		ConfigProcessing.processAnnotations(MODID, config, LibConfig.class);
		FMLCommonHandler.instance().bus().register(new ConfigChangeListener(MODID, config));

		if (config.hasChanged()) config.save();

		MinecraftForge.EVENT_BUS.register(DelayedEntityLoadManager.instance);

		MinecraftForge.EVENT_BUS.register(FakePlayerPool.instance);

		MinecraftForge.EVENT_BUS.register(DropCapture.instance);

		MinecraftForge.EVENT_BUS.register(BucketFillHandler.instance);

		MinecraftForge.EVENT_BUS.register(BlockDropsStore.instance.createForgeListener());

		FMLCommonHandler.instance().bus().register(DelayedActionTickHandler.INSTANCE);

		FMLCommonHandler.instance().bus().register(BlockDropsStore.instance.createFmlListener());

		FMLCommonHandler.instance().bus().register(ConfigStorage.instance);

		collector = new ClassSourceCollector(evt.getAsmData());

		EntityRegistry.registerModEntity(EntityBlock.class, "Block", ENTITY_BLOCK_ID, instance, 64, 1, true);

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
