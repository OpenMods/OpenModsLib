package openmods.calc.command;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import openmods.calc.Calculator.ExprType;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class CommandCalcEvaluate extends CommandCalcBase {
	private static final String NAME = "=eval";

	public CommandCalcEvaluate(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return NAME + " expr";
	}

	@Override
	public List<?> getCommandAliases() {
		return Lists.newArrayList("=evaluate", "=");
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		final String expr = spaceJoiner.join(args);
		try {
			if (state.exprType == ExprType.INFIX) {
				final String result = state.compileExecuteAndPrint(expr);
				sender.addChatMessage(new ChatComponentText(result));
			} else {
				state.compileAndExecute(expr);
				sender.addChatMessage(new ChatComponentText(spaceJoiner.join(state.getActiveCalculator().printStack())));
			}
		} catch (Exception e) {
			final List<String> causes = Lists.newArrayList();
			Throwable current = e;
			while (current != null) {
				causes.add(current.getMessage());
				current = current.getCause();
			}
			throw new CommandException("openmodslib.command.calc_error", Joiner.on("', caused by '").join(causes));
		}
	}

}
