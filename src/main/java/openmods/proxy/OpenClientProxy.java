package openmods.proxy;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import openmods.Log;
import openmods.OpenMods;
import openmods.block.BlockSelectionHandler;
import openmods.config.properties.CommandConfig;
import openmods.gui.ClientGuiHandler;
import openmods.movement.PlayerMovementManager;
import openmods.source.CommandSource;
import openmods.stencil.FramebufferConstants;
import openmods.stencil.StencilPoolManager;
import openmods.stencil.StencilPoolManager.StencilPool;
import openmods.utils.render.RenderUtils;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.network.IGuiHandler;

public final class OpenClientProxy implements IOpenModsProxy {

	@Override
	public EntityPlayer getThePlayer() {
		return FMLClientHandler.instance().getClient().thePlayer;
	}

	@Override
	public boolean isClientPlayer(Entity player) {
		return player instanceof EntityPlayerSP;
	}

	@Override
	public long getTicks(World worldObj) {
		if (worldObj != null) { return worldObj.getTotalWorldTime(); }
		World cWorld = getClientWorld();
		if (cWorld != null) return cWorld.getTotalWorldTime();
		return 0;
	}

	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().theWorld;
	}

	@Override
	public World getServerWorld(int id) {
		return DimensionManager.getWorld(id);
	}

	@Override
	public File getMinecraftDir() {
		return Minecraft.getMinecraft().mcDataDir;
	}

	@Override
	public String getLogFileName() {
		return "ForgeModLoader-client-0.log";
	}

	@Override
	public IGuiHandler wrapHandler(IGuiHandler modSpecificHandler) {
		return new ClientGuiHandler(modSpecificHandler);
	}

	@Override
	public void preInit() {
		ClientCommandHandler.instance.registerCommand(new CommandConfig("om_config_c", false));
		ClientCommandHandler.instance.registerCommand(new CommandSource("om_source_c", false, OpenMods.instance.getCollector()));

		RenderUtils.registerFogUpdater();

		MinecraftForge.EVENT_BUS.register(new BlockSelectionHandler());

		FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
			@Override
			public String call() throws Exception {
				final StencilPool pool = StencilPoolManager.pool();
				return String.format("Function set: %s, pool: %s, bits: %s", FramebufferConstants.getMethodSet(), pool.getType(), pool.getSize());
			}

			@Override
			public String getLabel() {
				return "Stencil buffer state";
			}
		});
	}

	@Override
	public void init() {}

	@Override
	public void postInit() {
		if (!PlayerMovementManager.isCallbackInjected()) {
			Log.info("EntityPlayerSP movement callback patch not applied, using legacy solution");
			FMLCommonHandler.instance().bus().register(new PlayerMovementManager.LegacyTickHandler());
		}
	}

	@Override
	public void setNowPlayingTitle(String nowPlaying) {
		Minecraft.getMinecraft().ingameGUI.setRecordPlayingMessage(nowPlaying);
	}

	@Override
	public EntityPlayer getPlayerFromHandler(INetHandler handler) {
		if (handler instanceof NetHandlerPlayServer) return ((NetHandlerPlayServer)handler).playerEntity;

		if (handler instanceof NetHandlerPlayClient) return getThePlayer();

		return null;
	}

}
