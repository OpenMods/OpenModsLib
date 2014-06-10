package openmods;

import openmods.config.properties.ConfigProperty;
import openmods.config.properties.OnLineModifiable;

public class LibConfig {
	@OnLineModifiable
	@ConfigProperty(category = "debug", name = "fakePlayerCountThreshold", comment = "Maximum fake player pool that can doesn't produce warning")
	public static int fakePlayerThreshold = 10;

}
