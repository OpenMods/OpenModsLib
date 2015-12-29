package openmods.calc.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.util.ChatComponentTranslation;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class CommandCalcFunction extends CommandCalcBase {
	private static final String NAME = "=func";

	public CommandCalcFunction(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return NAME + " name argCount expr";
	}

	@Override
	public List<String> getCommandAliases() {
		return Lists.newArrayList("=function");
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 3) throw new SyntaxErrorException();

		final String name = args[0];
		final int argCount;

		try {
			argCount = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new SyntaxErrorException();
		}
		if (argCount < 0) throw new SyntaxErrorException();

		final String expr = spaceJoiner.join(Iterables.skip(Arrays.asList(args), 2));
		state.compileAndDefineGlobalFunction(sender, name, argCount, expr);
		sender.addChatMessage(new ChatComponentTranslation("openmodslib.command.calc_function_defined", name));
	}
}
