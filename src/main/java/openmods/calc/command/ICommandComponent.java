package openmods.calc.command;

import java.util.List;
import net.minecraft.command.ICommandSender;

public interface ICommandComponent {
	public void execute(ICommandSender sender, IWhitespaceSplitter args);

	public ICommandComponent partialyExecute(IWhitespaceSplitter args);

	public List<String> getTabCompletions(IWhitespaceSplitter args);

	public void help(HelpPrinter printer);
}
