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

	@ConfigProperty(category = "control", name = "enableNaturalSpawnWhitelist", comment = "Should the natural spawn whitelist be enabled?")
	public static boolean enableNaturalSpawnWhitelist = false;
	
	@ConfigProperty(category = "control", name = "naturalSpawnWhitelist", comment = "List any mob names you will allow to spawn naturally")
	public static String[] naturalSpawnWhitelist = new String[0];
}
