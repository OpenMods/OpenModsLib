package openmods.calc.command;

import java.util.Arrays;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.util.ChatComponentTranslation;

import com.google.common.collect.Iterables;

public class CommandCalcLet extends CommandCalcBase {
	private static final String NAME = "=let";

	public CommandCalcLet(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return NAME + " name expr";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 2) throw new SyntaxErrorException();

		final String name = args[0];
		final String expr = spaceJoiner.join(Iterables.skip(Arrays.asList(args), 1));

		final Object result = state.compileAndSetGlobalSymbol(sender, name, expr);
		sender.addChatMessage(new ChatComponentTranslation("openmodslib.command.calc_set", name, String.valueOf(result)));
	}
}
