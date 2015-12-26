package openmods;

import openmods.config.properties.ConfigProperty;
import openmods.config.properties.OnLineModifiable;

public class LibConfig {
	@OnLineModifiable
	@ConfigProperty(category = "debug", name = "fakePlayerCountThreshold", comment = "Maximum fake player pool that doesn't produce warning")
	public static int fakePlayerThreshold = 10;

	@OnLineModifiable
	@ConfigProperty(category = "debug", name = "dropsDebug", comment = "Control printing of stacktraces in case of unharvested drops")
	public static boolean dropsDebug;

	@ConfigProperty(category = "feature", name = "calculatorCommands", comment = "Enables command line calculator")
	public static boolean enableCalculatorCommands = true;

	@ConfigProperty(category = "debug", name = "tessellatorPoolLimit", comment = "Maximum allowed size of tessellator pool")
	public static int tessellatorPoolLimit = 20;

}
