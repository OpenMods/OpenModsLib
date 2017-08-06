package openmods;

import java.io.File;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import openmods.config.ConfigChangeListener;
import openmods.config.ConfigStorage;
import openmods.config.properties.CommandConfig;
import openmods.config.properties.ConfigProcessing;
import openmods.entity.DelayedEntityLoadManager;
import openmods.entity.EntityBlock;
import openmods.fakeplayer.FakePlayerPool;
import openmods.integration.Integration;
import openmods.network.rpc.MethodEntry;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.TargetTypeProvider;
import openmods.network.rpc.targets.EntityRpcTarget;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.proxy.IOpenModsProxy;
import openmods.reflection.TypeVariableHolderHandler;
import openmods.source.ClassSourceCollector;
import openmods.source.CommandSource;
import openmods.sync.SyncChannelHolder;
import openmods.sync.SyncableBlock;
import openmods.sync.SyncableBlockState;
import openmods.sync.SyncableBoolean;
import openmods.sync.SyncableByte;
import openmods.sync.SyncableByteArray;
import openmods.sync.SyncableDouble;
import openmods.sync.SyncableEnum;
import openmods.sync.SyncableFlags;
import openmods.sync.SyncableFloat;
import openmods.sync.SyncableInt;
import openmods.sync.SyncableIntArray;
import openmods.sync.SyncableItemStack;
import openmods.sync.SyncableNBT;
import openmods.sync.SyncableObjectType;
import openmods.sync.SyncableObjectTypeRegistry;
import openmods.sync.SyncableShort;
import openmods.sync.SyncableSides;
import openmods.sync.SyncableString;
import openmods.sync.SyncableTank;
import openmods.sync.SyncableUUID;
import openmods.sync.SyncableUnsignedByte;
import openmods.sync.SyncableVarInt;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;
import openmods.world.DelayedActionTickHandler;
import openmods.world.DropCapture;

@Mod(modid = OpenMods.MODID, name = OpenMods.MODID, version = OpenMods.VERSION, dependencies = OpenMods.DEPENDENCIES, guiFactory = OpenMods.GUI_FACTORY, updateJSON = OpenMods.UPDATE_JSON)
public class OpenMods {

	public static final String MODID = "openmods";
	public static final String MODNAME = "OpenModsLib";
	public static final String VERSION = "$LIB-VERSION$";
	public static final String DEPENDENCIES = "required-after:openmodscore";
	public static final String GUI_FACTORY = "openmods.GuiFactory";
	public static final String UPDATE_JSON = "http://openmods.info/versions/openmodslib.json";

	private static final int ENTITY_BLOCK_ID = 804;

	@Instance(MODID)
	public static OpenMods instance;

	@SidedProxy(clientSide = "openmods.proxy.OpenClientProxy", serverSide = "openmods.proxy.OpenServerProxy")
	public static IOpenModsProxy proxy;

	private ClassSourceCollector collector;

	public ClassSourceCollector getCollector() {
		return collector;
	}

	public static ResourceLocation location(String id) {
		return new ResourceLocation(MODID, id);
	}

	@EventBusSubscriber
	public static class DefaultRegistryEntries {

		@SubscribeEvent
		public static void registerSyncTypes(RegistryEvent.Register<SyncableObjectType> type) {
			SyncableObjectTypeRegistry.startRegistration(type.getRegistry())
					.register(SyncableTank.class)
					.register(SyncableBlock.class)
					.register(SyncableBlockState.class)
					.register(SyncableBoolean.class)
					.register(SyncableByte.class)
					.register(SyncableByteArray.class)
					.register(SyncableDouble.class)
					.register(SyncableEnum.class, SyncableEnum.DUMMY_SUPPLIER)
					.register(SyncableFlags.ByteFlags.class)
					.register(SyncableFlags.ShortFlags.class)
					.register(SyncableFlags.IntFlags.class)
					.register(SyncableFloat.class)
					.register(SyncableInt.class)
					.register(SyncableIntArray.class)
					.register(SyncableItemStack.class)
					.register(SyncableNBT.class)
					.register(SyncableShort.class)
					.register(SyncableSides.class)
					.register(SyncableString.class)
					.register(SyncableUnsignedByte.class)
					.register(SyncableUUID.class)
					.register(SyncableVarInt.class);
		}

		@SubscribeEvent
		public static void registerMethodTypes(RegistryEvent.Register<MethodEntry> evt) {
			RpcCallDispatcher.startMethodRegistration(evt.getRegistry())
					.registerInterface(IRpcDirectionBitMap.class)
					.registerInterface(IRpcIntBitMap.class);
		}

		@SubscribeEvent
		public static void registerTargets(RegistryEvent.Register<TargetTypeProvider> evt) {
			RpcCallDispatcher.startTargetRegistration(evt.getRegistry())
					.registerTargetWrapper(EntityRpcTarget.class)
					.registerTargetWrapper(TileEntityRpcTarget.class)
					.registerTargetWrapper(SyncRpcTarget.SyncEntityRpcTarget.class)
					.registerTargetWrapper(SyncRpcTarget.SyncTileEntityRpcTarget.class);
		}

	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		new TypeVariableHolderHandler().fillAllHolders(evt.getAsmData());

		SyncChannelHolder.ensureLoaded();

		final File configFile = evt.getSuggestedConfigurationFile();
		Configuration config = new Configuration(configFile);
		ConfigProcessing.processAnnotations(MODID, config, LibConfig.class);
		MinecraftForge.EVENT_BUS.register(new ConfigChangeListener(MODID, config));

		if (config.hasChanged()) config.save();

		MinecraftForge.EVENT_BUS.register(DelayedEntityLoadManager.instance);

		MinecraftForge.EVENT_BUS.register(FakePlayerPool.instance);

		MinecraftForge.EVENT_BUS.register(DropCapture.instance);

		MinecraftForge.EVENT_BUS.register(DelayedActionTickHandler.INSTANCE);

		MinecraftForge.EVENT_BUS.register(ConfigStorage.instance);

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
		Integration.loadModules();
		proxy.postInit();
	}

	@EventHandler
	public void severStart(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandConfig("om_config_s", true));
		evt.registerServerCommand(new CommandSource("om_source_s", true, collector));
	}
}
