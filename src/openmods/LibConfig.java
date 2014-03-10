package openmods;

import openmods.config.ConfigProperty;
import openmods.config.OnLineModifiable;

public class LibConfig {

	@OnLineModifiable
	@ConfigProperty(category = "net", name = "debugLogPackets", comment = "PacketHandler will dump info about packets to separate file")
	public static boolean logPackets = false;

}
