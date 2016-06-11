package openmods.calc.command;

import com.google.common.base.Joiner;
import java.util.List;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

abstract class CommandCalcBase implements ICommand {
	protected final Joiner spaceJoiner = Joiner.on(' ');

	protected final CalcState state;

	protected final String name;

	public CommandCalcBase(String name, CalcState state) {
		this.name = name;
		this.state = state;
	}

	@Override
	public int compareTo(Object o) {
		return name.compareTo(((ICommand)o).getCommandName());
	}

	@Override
	public String getCommandName() {
		return name;
	}

	@Override
	public List<?> getCommandAliases() {
		return null;
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	@Override
	public List<?> addTabCompletionOptions(ICommandSender sender, String[] args) {
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}
}
