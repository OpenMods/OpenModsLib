package openmods.integration;

public interface IIntegrationModule {

	String name();

	boolean canLoad();

	void load();
}
