package openmods;

import openmods.config.ConfigProperty;
import openmods.config.OnLineModifiable;

public class LibConfig {

	@OnLineModifiable
	@ConfigProperty(category = "net", name = "debugLogPackets", comment = "PacketHandler will dump info about packets to separate file")
	public static boolean logPackets = false;

	@OnLineModifiable
	@ConfigProperty(category = "debug", name = "fakePlayerCountThreshold", comment = "Maximum fake player pool that can doesn't produce warning")
	public static int fakePlayerThreshold = 10;

}
