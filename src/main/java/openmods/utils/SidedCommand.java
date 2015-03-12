package openmods.utils;

import java.util.List;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

public abstract class SidedCommand implements ICommand {

	protected final String name;
	protected final boolean restricted;

	public SidedCommand(String name, boolean restricted) {
		this.name = name;
		this.restricted = restricted;
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
		int level = restricted? 4 : 0;
		return sender.canCommandSenderUseCommand(level, name);
	}
}
