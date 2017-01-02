package openmods.utils;

import java.io.File;
import openmods.OpenMods;

public class MCUtils {
	public static String getLogFileName() {
		return OpenMods.proxy.getLogFileName();
	}

	public static String getMinecraftDir() {
		return OpenMods.proxy.getMinecraftDir().getAbsolutePath();
	}

	public static String getConfigDir() {
		return new File(OpenMods.proxy.getMinecraftDir(), "config").getAbsolutePath();
	}
}
