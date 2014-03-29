package openmods.integration;

import cpw.mods.fml.common.Loader;

public abstract class ModIntegration implements IIntegrationModule {

	private final String modId;

	public ModIntegration(String modId) {
		this.modId = modId;
	}

	@Override
	public boolean canLoad() {
		return Loader.isModLoaded(modId);
	}
}
