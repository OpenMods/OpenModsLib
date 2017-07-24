package openmods.config.properties;

import static openmods.utils.CommandUtils.error;
import static openmods.utils.CommandUtils.filterPrefixes;
import static openmods.utils.CommandUtils.respond;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import openmods.Log;
import openmods.config.properties.ConfigProcessing.ModConfig;
import openmods.utils.SidedCommand;
import openmods.utils.io.StringConversionException;
import org.apache.commons.lang3.ArrayUtils;

public class CommandConfig extends SidedCommand {

	private static final String COMMAND_REMOVE = "remove";
	private static final String COMMAND_APPEND = "append";
	private static final String COMMAND_SET = "set";
	private static final String COMMAND_CLEAR = "clear";
	private static final String COMMAND_GET = "get";
	private static final String COMMAND_HELP = "help";
	private static final String COMMAND_SAVE = "save";
	private static final String COMMAND_DEFAULT = "default";

	private static final Set<String> SUBCOMMANDS = ImmutableSet.of(
			COMMAND_SAVE,
			COMMAND_HELP,
			COMMAND_GET,
			COMMAND_CLEAR,
			COMMAND_SET,
			COMMAND_APPEND,
			COMMAND_REMOVE,
			COMMAND_DEFAULT);

	public CommandConfig(String name, boolean restricted) {
		super(name, restricted);
	}

	@Override
	public String getUsage(ICommandSender icommandsender) {
		return name + " save <modid> OR\n" +
				name + " help <modid> <category> <name> OR\n" +
				name + " get <modid> <category> <name> OR\n" +
				name + " clear <modid> <category> <name> OR\n" +
				name + " set <modid> <category> <name> <value>... OR\n" +
				name + " append <modid> <category> <name> <value>... OR\n" +
				name + " remove <modid> <category> <name> <value>... OR\n" +
				name + " default <modid> <category> <name> <value>... OR\n";
	}

	private static void printValue(ICommandSender sender, final ModConfig config, ConfigPropertyMeta property) {
		respond(sender, "%s.%s.%s = %s (%s)", config.modId, property.category, property.name, property.valueDescription(), property.type.toString());
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 2) throw error("openmodslib.command.no_enough_args");

		final String command = args[0];
		if (!SUBCOMMANDS.contains(command)) throw error("openmodslib.command.invalid_command", command);

		final String modId = args[1];
		final ModConfig config = ConfigProcessing.getConfig(modId);
		if (config == null) throw error("openmodslib.command.unknown_modid");

		if (COMMAND_SAVE.equals(command)) {
			config.save();
			respond(sender, "openmodslib.command.saved", config.getConfigFile().getAbsolutePath());
			return;
		}

		if (args.length < 4) throw error("openmodslib.command.no_enough_args");

		final String category = args[2];
		final String name = args[3];

		ConfigPropertyMeta property = config.getValue(category, name);
		if (property == null) throw error("openmodslib.command.unknown_value");

		if (COMMAND_HELP.equals(command)) {
			respond(sender, "%s.%s.%s: %s (%s)", config.modId, property.category, property.name, property.comment, property.type.toString());
			return;
		} else if (COMMAND_GET.equals(command)) {
			printValue(sender, config, property);
			return;
		} else if (COMMAND_CLEAR.equals(command)) {
			if (property.acceptsMultipleValues()) changeValue(config, sender, property);
			else throw error("openmodslib.command.not_multiple");
			return;
		} else if (COMMAND_DEFAULT.equals(command)) {
			changeValue(config, sender, property, property.getDefaultValues());
			return;
		}

		if (args.length < 5) throw error("openmodslib.command.no_enough_args");

		String[] values = Arrays.copyOfRange(args, 4, args.length);

		if (COMMAND_SET.equals(command)) {
			changeValue(config, sender, property, values);
			return;
		}

		if (!property.acceptsMultipleValues()) throw error("openmodslib.command.not_multiple");

		String[] current = property.getPropertyValue();

		if (COMMAND_APPEND.equals(command)) {
			changeValue(config, sender, property, ArrayUtils.addAll(current, values));
			return;
		} else if (COMMAND_REMOVE.equals(command)) {
			changeValue(config, sender, property, ArrayUtils.removeElements(current, values));
			return;
		}

		throw error("openmodslib.command.no_enough_args");

	}

	protected void changeValue(ModConfig config, ICommandSender sender, ConfigPropertyMeta property, String... values) throws CommandException {
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
			throw error("openmodslib.command.invalid_type", Arrays.toString(values), property.type);
		} catch (Exception e) {
			Log.warn(e, "Error during command change");
			throw error("openmodslib.command.unknown_error", e.getMessage());
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos blockPos) {
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

	@Override
	public boolean isUsernameIndex(String[] astring, int i) {
		return false;
	}

}
