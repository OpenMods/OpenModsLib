package openmods.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

public class CommandUtils {

	public static List<String> filterPrefixes(String prefix, Iterable<String> proposals) {
		prefix = prefix.toLowerCase(Locale.ENGLISH);

		List<String> result = Lists.newArrayList();
		for (String s : proposals)
			if (s.toLowerCase(Locale.ENGLISH).startsWith(prefix)) result.add(s);

		return result;
	}

	public static List<String> filterPrefixes(String prefix, String... proposals) {
		return filterPrefixes(prefix, Arrays.asList(proposals));
	}

	public static List<String> getPlayerNames() {
		MinecraftServer server = MinecraftServer.getServer();
		if (server != null) return ImmutableList.copyOf(server.getAllUsernames());
		return ImmutableList.of();
	}

	public static List<String> fiterPlayerNames(String prefix) {
		return filterPrefixes(prefix, getPlayerNames());
	}

	public static void respondText(ICommandSender sender, String message) {
		sender.addChatMessage(new ChatComponentText(message));
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

		player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playerName);
		if (player != null) return player;

		throw new PlayerNotFoundException();
	}
}
