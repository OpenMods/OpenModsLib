package openmods;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
import openmods.proxy.OpenClientProxy;
import openmods.proxy.OpenServerProxy;
import openmods.recipe.EnchantingRecipe;
import openmods.reflection.TypeVariableHolderHandler;
import openmods.source.ClassSourceCollector;
import openmods.sync.SyncChannelHolder;
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

@Mod(OpenMods.MODID)
public class OpenMods {

	public static final String MODID = "openmods";

	public static final IOpenModsProxy proxy = DistExecutor.runForDist(() -> OpenClientProxy::new, () -> OpenServerProxy::new);
	public static final String ENTITY_BLOCK_ID = MODID + ":block";

	private ClassSourceCollector collector;

	public ClassSourceCollector getCollector() {
		return collector;
	}

	public static ResourceLocation location(String id) {
		return new ResourceLocation(MODID, id);
	}

	public OpenMods() {
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::preInit);
		modEventBus.addListener(this::postInit);

		MinecraftForge.EVENT_BUS.addListener(this::severStart);
	}

	@EventBusSubscriber
	public static class DefaultRegistryEntries {

		@SubscribeEvent
		public static void registerSyncTypes(RegistryEvent.Register<SyncableObjectType> type) {
			SyncableObjectTypeRegistry.startRegistration(type.getRegistry())
					.register(SyncableTank.class)
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

		@SubscribeEvent
		public static void registerEntities(RegistryEvent.Register<EntityType<?>> evt) {
			evt.getRegistry().register(
					EntityType.Builder.create(EntityBlock::new, EntityClassification.MISC)
							.setTrackingRange(64)
							.setUpdateInterval(1)
							.size(0.925F, 0.925F)
							.setCustomClientFactory((spawnEntity, world) -> new EntityBlock(EntityBlock.TYPE, world))
							.build(ENTITY_BLOCK_ID)
			);
		}
	}

	private void preInit(FMLCommonSetupEvent evt) {
		new TypeVariableHolderHandler().fillAllHolders();

		SyncChannelHolder.ensureLoaded();

		// TODO: 1.14 config handling
		//final File configFile = evt.getSuggestedConfigurationFile();
		//Configuration config = new Configuration(configFile);
		//ConfigProcessing.processAnnotations(MODID, config, LibConfig.class);
		//MinecraftForge.EVENT_BUS.register(new ConfigChangeListener(MODID, config));

		//if (config.hasChanged()) config.save();

		MinecraftForge.EVENT_BUS.register(DelayedEntityLoadManager.instance);

		MinecraftForge.EVENT_BUS.register(FakePlayerPool.instance);

		MinecraftForge.EVENT_BUS.register(DropCapture.instance);

		MinecraftForge.EVENT_BUS.register(DelayedActionTickHandler.INSTANCE);

		MinecraftForge.EVENT_BUS.register(EnchantingRecipe.class);

		collector = new ClassSourceCollector();

		CraftingHelper.register(new EnchantingRecipe.EchantmentExistsConditionSerializer());

		proxy.preInit();
	}

	private void postInit(InterModProcessEvent evt) {
		Integration.loadModules();
		proxy.postInit();
	}

	private void severStart(FMLServerStartingEvent evt) {
		// TODO 1.14 Redo commands
		//evt.registerServerCommand(new CommandConfig("om_config_s", true));
		//evt.registerServerCommand(new CommandSource("om_source_s", true, collector));
	}
}
