package openmods.api;

public interface IProxy {
	public void preInit();

	public void init();

	public void postInit();

	public void registerRenderInformation();
}
