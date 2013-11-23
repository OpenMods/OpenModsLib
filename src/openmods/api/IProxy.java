package openmods.api;

import cpw.mods.fml.common.network.IGuiHandler;

public interface IProxy {

	public IGuiHandler createGuiHandler();

	public void preInit();

	public void init();

	public void postInit();

	public void registerRenderInformation();
}
