package openmods.calc;

import java.util.List;
import net.minecraft.command.ICommandSender;

public interface ICommandComponent {
	void execute(ICommandSender sender, IWhitespaceSplitter args);

	ICommandComponent partialyExecute(IWhitespaceSplitter args);

	List<String> getTabCompletions(IWhitespaceSplitter args);

	void help(HelpPrinter printer);
}
