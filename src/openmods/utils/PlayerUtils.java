package openmods.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import cpw.mods.fml.common.FMLCommonHandler;

public class PlayerUtils {
	public static boolean isPlayerOp(String username) {
		username = username.toLowerCase();

		MinecraftServer server = FMLCommonHandler.instance().getSidedDelegate().getServer();

		// SP and LAN
		if (server.isSinglePlayer()) {
			if (server instanceof IntegratedServer) return server.getServerOwner().equals(username);
			return server.getConfigurationManager().getOps().contains(username);
		}

		// SMP
		return server.getConfigurationManager().getOps().contains(username);
	}
}
