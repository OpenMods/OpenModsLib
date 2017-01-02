package openmods.calc.command;

import java.util.List;

public abstract class TerminalCommandComponent implements ICommandComponent {

	private final String help;

	public TerminalCommandComponent(String help) {
		this.help = help;
	}

	@Override
	public ICommandComponent partialyExecute(IWhitespaceSplitter args) {
		throw new CommandSyntaxException("openmodslib.command.no_subcommands");
	}

	@Override
	public List<String> getTabCompletions(IWhitespaceSplitter args) {
		return null;
	}

	@Override
	public void help(HelpPrinter printer) {
		printer.print(help);
	}

}
