package openmods.config;

import java.util.*;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatMessageComponent;
import openmods.Log;
import openmods.config.ConfigProcessing.ModConfig;
import openmods.utils.io.StringConversionException;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class CommandConfig implements ICommand {

	private static final String COMMAND_REMOVE = "remove";
	private static final String COMMAND_APPEND = "append";
	private static final String COMMAND_SET = "set";
	private static final String COMMAND_CLEAR = "clear";
	private static final String COMMAND_GET = "get";
	private static final String COMMAND_HELP = "help";
	private static final String COMMAND_SAVE = "save";

	private static final Set<String> SUBCOMMANDS = ImmutableSet.of(
			COMMAND_SAVE,
			COMMAND_HELP,
			COMMAND_GET,
			COMMAND_CLEAR,
			COMMAND_SET,
			COMMAND_APPEND,
			COMMAND_REMOVE);

	public CommandConfig(String name, boolean restricted) {
		this.name = name;
		this.restricted = restricted;
	}

	private final String name;
	private final boolean restricted;

	@Override
	public int compareTo(Object o) {
		return name.compareTo(((ICommand)o).getCommandName());
	}

	@Override
	public String getCommandName() {
		return name;
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return name + " save <modid> OR\n" +
				name + " help <modid> <category> <name> OR\n" +
				name + " get <modid> <category> <name> OR\n" +
				name + " clear <modid> <category> <name> OR\n" +
				name + " set <modid> <category> <name> <value>... OR\n" +
				name + " append <modid> <category> <name> <value>... OR\n" +
				name + " remove <modid> <category> <name> <value>...";
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List getCommandAliases() {
		return null;
	}

	private static void respond(ICommandSender sender, String format) {
		sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey(format));
	}

	private static void respond(ICommandSender sender, String format, Object... args) {
		sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(format, args));
	}

	private static void printValue(ICommandSender sender, final ModConfig config, ConfigPropertyMeta property) {
		respond(sender, "%s.%s.%s = %s (%s)", config.modId, property.category, property.name, property.valueDescription(), property.type.toString());
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length < 2) {
			respond(sender, "openmodslib.command.no_enough_args");
			return;
		}

		final String command = args[0];

		if (!SUBCOMMANDS.contains(command)) {
			respond(sender, "openmodslib.command.invalid_command", command);
			return;
		}

		final String modId = args[1];
		final ModConfig config = ConfigProcessing.getConfig(modId);
		if (config == null) {
			respond(sender, "openmodslib.command.unknown_modid");
			return;
		}

		if (COMMAND_SAVE.equals(command)) {
			config.save();
			respond(sender, "openmodslib.command.saved", config.configFile.getAbsolutePath());
			return;
		}

		if (args.length < 4) {
			respond(sender, "openmodslib.command.no_enough_args");
			return;
		}

		final String category = args[2];
		final String name = args[3];

		ConfigPropertyMeta property = config.getValue(category, name);
		if (property == null) {
			respond(sender, "openmodslib.command.unknown_value");
			return;
		}

		if (COMMAND_HELP.equals(command)) {
			respond(sender, "%s.%s.%s: %s (%s)", config.modId, property.category, property.name, property.comment, property.type.toString());
			return;
		} else if (COMMAND_GET.equals(command)) {
			printValue(sender, config, property);
			return;
		} else if (COMMAND_CLEAR.equals(command)) {
			if (property.acceptsMultipleValues()) changeValue(config, sender, property);
			else respond(sender, "openmodslib.command.not_multiple");

			return;
		}

		if (args.length < 5) {
			respond(sender, "openmodslib.command.no_enough_args");
			return;
		}

		String[] values = Arrays.copyOfRange(args, 4, args.length);

		if (COMMAND_SET.equals(command)) {
			changeValue(config, sender, property, values);
			return;
		}

		if (!property.acceptsMultipleValues()) {
			respond(sender, "openmodslib.command.not_multiple");
			return;
		}

		String[] current = property.getPropertyValue();

		if (COMMAND_APPEND.equals(command)) {
			changeValue(config, sender, property, ArrayUtils.addAll(current, values));
			return;
		} else if (COMMAND_REMOVE.equals(command)) {
			changeValue(config, sender, property, ArrayUtils.removeElements(current, values));
			return;
		}

		respond(sender, "openmodslib.command.no_enough_args");

	}

	protected void changeValue(ModConfig config, ICommandSender sender, ConfigPropertyMeta property, String... values) {
		try {
			ConfigPropertyMeta.Result changeResult = property.tryChangeValue(values);
			switch (changeResult) {
				case ONLINE:
					respond(sender, "openmodslib.command.online_change");
					break;
				case OFFLINE:
					respond(sender, "openmodslib.command.offline_change");
					break;
				default:
					respond(sender, "openmodslib.command.cancelled");
					break;
			}
			printValue(sender, config, property);
		} catch (StringConversionException e) {
			respond(sender, "openmodslib.command.invalid_type", Arrays.toString(values), property.type);
		} catch (Exception e) {
			respond(sender, "openmodslib.command.unknown_error", e.getMessage());
			Log.warn(e, "Error during command change");
		}
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		int level = restricted? 4 : 0;
		return sender.canCommandSenderUseCommand(level, name);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		String command = args[0];
		if (args.length == 1) return filterPrefixes(command, SUBCOMMANDS);

		String modId = args[1];
		if (args.length == 2) return filterPrefixes(modId, ConfigProcessing.getConfigsIds());

		if (COMMAND_SAVE.equals(command)) return null;

		final ModConfig config = ConfigProcessing.getConfig(modId);
		if (config == null) return null;

		String category = args[2];
		if (args.length == 3) return filterPrefixes(category, config.getCategories());

		String name = args[3];
		if (args.length == 4) return filterPrefixes(name, config.getValues(category));

		return null;
	}

	private static List<String> filterPrefixes(String prefix, Collection<String> proposals) {
		prefix = prefix.toLowerCase();

		List<String> result = Lists.newArrayList();
		for (String s : proposals)
			if (s.startsWith(prefix)) result.add(s);

		return result;
	}

	@Override
	public boolean isUsernameIndex(String[] astring, int i) {
		return false;
	}

}
