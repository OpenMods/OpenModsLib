package openmods.proxy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.LibConfig;
import openmods.OpenMods;
import openmods.block.BlockSelectionHandler;
import openmods.geometry.HitboxManager;
import openmods.geometry.IHitboxSupplier;
import openmods.gui.ClientGuiHandler;
import openmods.model.MappedModelLoader;
import openmods.model.ModelWithDependencies;
import openmods.model.MultiLayerModel;
import openmods.model.PerspectiveAwareModel;
import openmods.model.eval.EvalExpandModel;
import openmods.model.eval.EvalModel;
import openmods.model.itemstate.ItemStateModel;
import openmods.model.textureditem.TexturedItemModel;
import openmods.model.variant.VariantModel;
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
	public World getServerWorld(final DimensionType id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.fromNullable(getClient().gameSettings.language);
	}

	@Override
	public IGuiHandler wrapHandler(IGuiHandler modSpecificHandler) {
		return new ClientGuiHandler(modSpecificHandler);
	}

	@Override
	public void preInit() {
		//ClientCommandHandler.instance.registerCommand(new CommandConfig("om_config_c", false));
		//ClientCommandHandler.instance.registerCommand(new CommandSource("om_source_c", false, OpenMods.instance.getCollector()));
		//ClientCommandHandler.instance.registerCommand(new CommandGlDebug());

		if (LibConfig.enableCalculatorCommands) {
			// TODO register calc command
		}

		RenderUtils.registerFogUpdater();

		MinecraftForge.EVENT_BUS.register(new BlockSelectionHandler());

		ModelLoaderRegistry.registerLoader(MappedModelLoader.builder()
				.put("with-dependencies", ModelWithDependencies.EMPTY)
				.put("multi-layer", MultiLayerModel.EMPTY)
				.put("variantmodel", VariantModel.EMPTY_MODEL)
				.put("textureditem", TexturedItemModel.INSTANCE)
				.put("stateitem", ItemStateModel.EMPTY)
				.put("eval", EvalModel.EMPTY)
				.put("eval-expand", EvalExpandModel.EMPTY)
				.put("perspective-aware", PerspectiveAwareModel.EMPTY)
				.build(OpenMods.MODID));

		// TODO 1.14 investigate thread safety
		((IReloadableResourceManager)getClient().getResourceManager()).addReloadListener(hitboxManager);

		// Stuff to run on main thread
		Minecraft.getInstance().deferTask(FramebufferBlitter::setup);
	}

	@Override
	public void postInit() {}

	@Override
	public void setNowPlayingTitle(String nowPlaying) {
		getClient().ingameGUI.setRecordPlayingMessage(nowPlaying);
	}

	@Override
	public IHitboxSupplier getHitboxes(ResourceLocation location) {
		return hitboxManager.get(location);
	}

	@Override
	public IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters) {
		return ModelLoaderRegistry.loadASM(location, parameters);
	}

}
