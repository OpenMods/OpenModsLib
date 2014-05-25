package openmods.utils;

import java.util.Collection;
import java.util.List;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CommandUtils {

	public static List<String> filterPrefixes(String prefix, Collection<String> proposals) {
		prefix = prefix.toLowerCase();

		List<String> result = Lists.newArrayList();
		for (String s : proposals)
			if (s.startsWith(prefix)) result.add(s);

		return result;
	}

	public static List<String> getPlayerNames() {
		MinecraftServer server = MinecraftServer.getServer();
		if (server != null) return ImmutableList.copyOf(server.getAllUsernames());
		return ImmutableList.of();
	}

	public static List<String> fiterPlayerNames(String prefix) {
		return filterPrefixes(prefix, getPlayerNames());
	}

	public static void respond(ICommandSender sender, String format, Object... args) {
		sender.addChatMessage(new ChatComponentTranslation(format, args));
	}

	public static CommandException error(String format, Object... args) {
		throw new CommandException(format, args);
	}

	public static EntityPlayerMP getPlayer(ICommandSender sender, String playerName) {
		EntityPlayerMP player = PlayerSelector.matchOnePlayer(sender, playerName);
		if (player != null) return player;

		player = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(playerName);
		if (player != null) return player;

		throw new PlayerNotFoundException();
	}
}
