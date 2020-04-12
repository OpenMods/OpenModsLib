package openmods.proxy;

import com.google.common.base.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import openmods.LibConfig;
import openmods.OpenMods;
import openmods.block.BlockSelectionHandler;
import openmods.geometry.HitboxManager;
import openmods.geometry.IHitboxSupplier;
import openmods.model.variant.VariantModelLoader;
import openmods.utils.render.FramebufferBlitter;
import openmods.utils.render.RenderUtils;

public final class OpenClientProxy implements IOpenModsProxy {
	private static Minecraft getClient() {
		return LogicalSidedProvider.INSTANCE.get(LogicalSide.CLIENT);
	}

	private final HitboxManager hitboxManager = new HitboxManager();

	@Override
	public World getClientWorld() {
		return getClient().world;
	}

	@Override
	public World getServerWorld(final RegistryKey<World> id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.fromNullable(getClient().gameSettings.language);
	}

	@Override
	public void eventInit() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onModelRegister);
	}

	@Override
	public void earlySyncInit() {
		final Minecraft client = Minecraft.getInstance();
		// May happen in datagen
		if (client != null) {
			((IReloadableResourceManager)client.getResourceManager()).addReloadListener(hitboxManager);
		}
	}

	@Override
	public void clientInit() {
		//ClientCommandHandler.instance.registerCommand(new CommandConfig("om_config_c", false));
		//ClientCommandHandler.instance.registerCommand(new CommandSource("om_source_c", false, OpenMods.instance.getCollector()));
		//ClientCommandHandler.instance.registerCommand(new CommandGlDebug());

		if (LibConfig.enableCalculatorCommands) {
			// TODO register calc command
		}

		RenderUtils.registerFogUpdater();

		MinecraftForge.EVENT_BUS.register(new BlockSelectionHandler());

		// Stuff to run on main thread
		Minecraft.getInstance().deferTask(FramebufferBlitter::setup);
	}

	private void onModelRegister(ModelRegistryEvent evt) {
		ModelLoaderRegistry.registerLoader(OpenMods.location("variant"), new VariantModelLoader());

		//ModelLoaderRegistry.registerLoader(OpenMods.location("textureditem"), new TexturedModelLoader());

		//		ModelLoaderRegistry.registerLoader(MappedModelLoader.builder()
		//				.put("stateitem", ItemStateModel.EMPTY)
		//				.put("eval", EvalModel.EMPTY)
		//				.put("eval-expand", EvalExpandModel.EMPTY)
		//				.build(OpenMods.MODID));
	}

	@Override
	public void setNowPlayingTitle(ITextComponent nowPlaying) {
		getClient().ingameGUI.func_238451_a_(nowPlaying);
	}

	@Override
	public IHitboxSupplier getHitboxes(ResourceLocation location) {
		return hitboxManager.get(location);
	}

//	@Override
//	public IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters) {
//		return ModelLoaderRegistry.loadASM(location, parameters);
//	}

}
