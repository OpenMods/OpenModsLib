package openmods;

import openmods.config.ConfigProperty;

public class LibConfig {

	@ConfigProperty(category = "net", name = "debugLogPackets", comment = "PacketHandler will dump info about packets to separate file")
	public static boolean logPackets = false;

}
