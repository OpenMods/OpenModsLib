package openmods.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

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

	public static List<String> getPlayerNames(MinecraftServer server) {
		if (server != null) return ImmutableList.copyOf(server.getOnlinePlayerNames());
		return ImmutableList.of();
	}

	public static List<String> fiterPlayerNames(MinecraftServer server, String prefix) {
		return filterPrefixes(prefix, getPlayerNames(server));
	}

	public static void respondText(ICommandSender sender, String message) {
		sender.sendMessage(new StringTextComponent(message));
	}

	public static void respond(ICommandSender sender, String format, Object... args) {
		sender.sendMessage(new TranslationTextComponent(format, args));
	}

	public static CommandException error(String format, Object... args) throws CommandException {
		throw new CommandException(format, args);
	}

}
