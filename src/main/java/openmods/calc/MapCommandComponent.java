package openmods.calc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.ICommandSender;
import openmods.utils.CommandUtils;

public class MapCommandComponent implements ICommandComponent {

	private final Map<String, ICommandComponent> subCommands;

	private MapCommandComponent(Map<String, ICommandComponent> subCommands) {
		this.subCommands = subCommands;
	}

	@Override
	public void execute(ICommandSender sender, IWhitespaceSplitter args) {
		final String key = args.getNextPart();

		final ICommandComponent subCommand = subCommands.get(key);
		if (subCommand == null) throw new CommandSyntaxException("openmodslib.command.no_subcommand", key);

		try {
			subCommand.execute(sender, args);
		} catch (NestedCommandException e) {
			throw e.pushCommandName(key);
		} catch (Exception e) {
			throw new CommandExecutionException(e).pushCommandName(key);
		}
	}

	@Override
	public ICommandComponent partialyExecute(IWhitespaceSplitter args) {
		final String key = args.getNextPart();
		final ICommandComponent subCommand = subCommands.get(key);
		if (subCommand == null) throw new CommandSyntaxException("openmodslib.command.no_subcommand", key);
		return subCommand;
	}

	@Override
	public void help(HelpPrinter printer) {
		for (Map.Entry<String, ICommandComponent> e : subCommands.entrySet()) {
			printer.push(e.getKey());
			e.getValue().help(printer);
			printer.pop();
		}
	}

	@Override
	public List<String> getTabCompletions(IWhitespaceSplitter args) {
		final String key = args.getNextPart();
		if (args.isFinished())
			return CommandUtils.filterPrefixes(key, subCommands.keySet());
		else {
			final ICommandComponent subCommand = subCommands.get(key);
			if (subCommand == null) return ImmutableList.of();
			return subCommand.getTabCompletions(args);
		}
	}

	public static class Builder {
		private ImmutableMap.Builder<String, ICommandComponent> builder = ImmutableMap.builder();

		public Builder put(String command, ICommandComponent commandComponent) {
			builder.put(command, commandComponent);
			return this;
		}

		public ICommandComponent build() {
			return new MapCommandComponent(builder.build());
		}
	}

	public static Builder builder() {
		return new Builder();
	}
}
