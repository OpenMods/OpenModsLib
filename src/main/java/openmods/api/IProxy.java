package openmods.api;

public interface IProxy {
	void preInit();

	void init();

	void postInit();

	void registerRenderInformation();
}
