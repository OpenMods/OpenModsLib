package openmods.utils;

import java.util.Collections;
import java.util.List;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public abstract class SidedCommand implements ICommand {

	protected final String name;
	protected final boolean restricted;

	public SidedCommand(String name, boolean restricted) {
		this.name = name;
		this.restricted = restricted;
	}

	@Override
	public int compareTo(ICommand o) {
		return name.compareTo(o.getName());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<String> getAliases() {
		return Collections.emptyList();
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return restricted? sender.canUseCommand(4, name) : true;
	}
}
