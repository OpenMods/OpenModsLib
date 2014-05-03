package openmods.integration;

public interface IIntegrationModule {

	public String name();

	public boolean canLoad();

	public void load();
}
