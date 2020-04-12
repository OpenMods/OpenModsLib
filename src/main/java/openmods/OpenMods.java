package openmods;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
import openmods.sync.drops.SyncDropsFunction;
import openmods.utils.bitmap.IRpcDirectionBitMap;
import openmods.utils.bitmap.IRpcIntBitMap;
import openmods.world.DelayedActionTickHandler;
import openmods.world.DropCapture;

@Mod(OpenMods.MODID)
public class OpenMods {

	public static final String MODID = "openmods";

	public static final IOpenModsProxy PROXY = DistExecutor.runForDist(() -> OpenClientProxy::new, () -> OpenServerProxy::new);
	public static final String ENTITY_BLOCK = MODID + ":block";

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
		modEventBus.addListener(this::clientInit);
		modEventBus.addListener(this::processImc);
		modEventBus.addListener(this::registerRegistry);

		MinecraftForge.EVENT_BUS.addListener(this::severStart);
	}

	private void registerRegistry(RegistryEvent.NewRegistry e) {
		// Now this not a right place for that, but only one that works:
		// - it's fired synchronously
		// - before most things (like resource managers) have chance to run
		PROXY.earlySyncInit();
		// It should be in registry!
		LootFunctionManager.registerFunction(new SyncDropsFunction.Serializer());
	}

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class DefaultRegistryEntries {

		@SubscribeEvent
		public static void registerSyncTypes(RegistryEvent.Register<SyncableObjectType> type) {
			SyncableObjectTypeRegistry.startRegistration(type.getRegistry())
					.register(location("tank"), SyncableTank.class, SyncableTank::new)
					.register(location("block_state"), SyncableBlockState.class, SyncableBlockState::new)
					.register(location("bool"), SyncableBoolean.class, SyncableBoolean::new)
					.register(location("byte"), SyncableByte.class, SyncableByte::new)
					.register(location("byte_array"), SyncableByteArray.class, SyncableByteArray::new)
					.register(location("double"), SyncableDouble.class, SyncableDouble::new)
					.register(location("enum"), SyncableEnum.class, SyncableEnum.DUMMY_SUPPLIER)
					.register(location("flags8"), SyncableFlags.ByteFlags.class, SyncableFlags.ByteFlags::new)
					.register(location("flags16"), SyncableFlags.ShortFlags.class, SyncableFlags.ShortFlags::new)
					.register(location("flags32"), SyncableFlags.IntFlags.class, SyncableFlags.IntFlags::new)
					.register(location("float"), SyncableFloat.class, SyncableFloat::new)
					.register(location("int"), SyncableInt.class, SyncableInt::new)
					.register(location("int_array"), SyncableIntArray.class, SyncableIntArray::new)
					.register(location("item_stack"), SyncableItemStack.class, SyncableItemStack::new)
					.register(location("nbt"), SyncableNBT.class, SyncableNBT::new)
					.register(location("short"), SyncableShort.class, SyncableShort::new)
					.register(location("sides"), SyncableSides.class, SyncableSides::new)
					.register(location("string"), SyncableString.class, SyncableString::new)
					.register(location("ubyte"), SyncableUnsignedByte.class, SyncableUnsignedByte::new)
					.register(location("uuid"), SyncableUUID.class, SyncableUUID::new)
					.register(location("var_int"), SyncableVarInt.class, SyncableVarInt::new);
		}

		@SubscribeEvent
		public static void registerMethodTypes(RegistryEvent.Register<MethodEntry> evt) {
			RpcCallDispatcher.startMethodRegistration(evt.getRegistry())
					.registerInterface(location("dir_bit_map"), IRpcDirectionBitMap.class)
					.registerInterface(location("int_bit_map"), IRpcIntBitMap.class);
		}

		@SubscribeEvent
		public static void registerTargets(RegistryEvent.Register<TargetTypeProvider> evt) {
			RpcCallDispatcher.startTargetRegistration(evt.getRegistry())
					.registerTargetWrapper(location("entity"), EntityRpcTarget.class, EntityRpcTarget::new)
					.registerTargetWrapper(location("tile_entity"), TileEntityRpcTarget.class, TileEntityRpcTarget::new)
					.registerTargetWrapper(location("sync_entity"), SyncRpcTarget.SyncEntityRpcTarget.class, SyncRpcTarget.SyncEntityRpcTarget::new)
					.registerTargetWrapper(location("sync_tile_entity"), SyncRpcTarget.SyncTileEntityRpcTarget.class, SyncRpcTarget.SyncTileEntityRpcTarget::new);
		}

		@SubscribeEvent
		public static void registerEntities(RegistryEvent.Register<EntityType<?>> evt) {
			evt.getRegistry().register(
					EntityType.Builder.create(EntityBlock::new, EntityClassification.MISC)
							.setTrackingRange(64)
							.setUpdateInterval(1)
							.size(0.925F, 0.925F)
							.setCustomClientFactory((spawnEntity, world) -> new EntityBlock(EntityBlock.TYPE, world))
							.build("")
							.setRegistryName(ENTITY_BLOCK)
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
	}

	public void clientInit(FMLClientSetupEvent evt) {
		PROXY.clientInit();
	}

	private void processImc(InterModProcessEvent evt) {
		Integration.loadModules();
	}

	private void severStart(FMLServerStartingEvent evt) {
		// TODO 1.14 Redo commands
		//evt.registerServerCommand(new CommandConfig("om_config_s", true));
		//evt.registerServerCommand(new CommandSource("om_source_s", true, collector));
	}
}
