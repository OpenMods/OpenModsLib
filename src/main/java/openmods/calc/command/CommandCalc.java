package openmods.calc.command;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import openmods.calc.Calculator.ExprType;

import com.google.common.collect.Lists;

public class CommandCalc extends CommandBase {
	private static final String NAME = "=";

	public CommandCalc(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return NAME + " expr";
	}

	@Override
	public List<?> getCommandAliases() {
		return Lists.newArrayList("eval");
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		final String expr = spaceJoiner.join(args);
		if (state.exprType == ExprType.INFIX) {
			final Object result = state.compileExecuteAndPop(expr);
			sender.addChatMessage(new ChatComponentText(String.valueOf(result)));
		} else {
			state.compileAndExecute(expr);
			sender.addChatMessage(new ChatComponentText(spaceJoiner.join(state.getActiveCalculator().getStack())));
		}
	}

}
