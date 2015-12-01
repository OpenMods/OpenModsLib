package openmods.calc.command;

import java.util.Arrays;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;

import com.google.common.collect.Iterables;

public class CommandFunction extends CommandBase {
	private static final String NAME = "function";

	public CommandFunction(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return NAME + " name argCount expr";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length < 3) throw new SyntaxErrorException();

		final String name = args[0];
		final int argCount;

		try {
			argCount = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new SyntaxErrorException();
		}
		final String expr = spaceJoiner.join(Iterables.skip(Arrays.asList(args), 2));
		state.compileAndDefineGlobalFunction(name, argCount, expr);
	}
}
