package openmods.calc.command;

import java.util.List;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import com.google.common.base.Joiner;

abstract class CommandCalcBase implements ICommand {
	protected final Joiner spaceJoiner = Joiner.on(' ');

	protected final CalcState state;

	protected final String name;

	public CommandCalcBase(String name, CalcState state) {
		this.name = name;
		this.state = state;
	}

	@Override
	public int compareTo(ICommand o) {
		return name.compareTo(o.getCommandName());
	}

	@Override
	public String getCommandName() {
		return name;
	}

	@Override
	public List<String> getCommandAliases() {
		return null;
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}
}
