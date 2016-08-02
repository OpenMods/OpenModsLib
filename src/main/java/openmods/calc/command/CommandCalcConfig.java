package openmods.calc.command;

import static openmods.utils.CommandUtils.error;
import static openmods.utils.CommandUtils.filterPrefixes;
import static openmods.utils.CommandUtils.respond;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import openmods.calc.ExprType;
import openmods.calc.command.CalcState.CalculatorType;
import openmods.calc.command.CalcState.NoSuchNameException;
import openmods.config.simpler.ConfigurableClassAdapter.NoSuchPropertyException;
import openmods.utils.Stack.StackUnderflowException;

public class CommandCalcConfig extends CommandCalcBase {
	private static final String NAME = "=config";

	private static final String COMMAND_LOAD = "load";
	private static final String COMMAND_STORE = "store";
	private static final String COMMAND_POP = "pop";
	private static final String COMMAND_PUSH = "push";
	private static final String COMMAND_NEW = "new";
	private static final String COMMAND_GET = "get";
	private static final String COMMAND_SET = "set";
	private static final String COMMAND_MODE = "mode";

	private static Set<String> SUB_COMMANDS = ImmutableSet.of(
			COMMAND_NEW,
			COMMAND_POP, COMMAND_PUSH,
			COMMAND_LOAD, COMMAND_STORE,
			COMMAND_GET, COMMAND_SET,
			COMMAND_MODE);

	public CommandCalcConfig(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return NAME + " " + COMMAND_NEW + " " + Arrays.toString(CalculatorType.values()) + " OR " +
				NAME + " " + COMMAND_PUSH + " OR " +
				NAME + " " + COMMAND_POP + " OR " +
				NAME + " " + COMMAND_STORE + " <name> OR " +
				NAME + " " + COMMAND_LOAD + " <name> OR " +
				NAME + " " + COMMAND_GET + " <option> OR" +
				NAME + " " + COMMAND_SET + " <option> <value> OR " +
				NAME + " " + COMMAND_MODE + " " + Arrays.toString(ExprType.values());
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length < 1) throw new SyntaxErrorException();

		final String subCommand = args[0];

		if (subCommand.equalsIgnoreCase(COMMAND_NEW)) {
			if (args.length < 2) throw error("openmodslib.command.calc_no_type");
			final String type = args[1].toUpperCase(Locale.ENGLISH);
			try {
				final CalculatorType newType = CalculatorType.valueOf(type);
				state.createCalculator(newType);
			} catch (IllegalArgumentException e) {
				throw error("openmodslib.command.calc_invalid_type", Joiner.on(',').join(CalculatorType.values()));
			}
		} else if (subCommand.equalsIgnoreCase(COMMAND_PUSH)) {
			final int size = state.pushCalculator();
			respond(sender, "openmodslib.command.calc_after_push", size);
		} else if (subCommand.equalsIgnoreCase(COMMAND_POP)) {
			try {
				final int size = state.popCalculator();
				respond(sender, "openmodslib.command.calc_after_pop", size);
			} catch (StackUnderflowException e) {
				error("openmodslib.command.calc_stack_underflow");
			}
		} else if (subCommand.equalsIgnoreCase(COMMAND_STORE)) {
			if (args.length < 2) throw error("openmodslib.command.calc_no_name");
			final String name = args[1];
			state.nameCalculator(name);
		} else if (subCommand.equalsIgnoreCase(COMMAND_LOAD)) {
			if (args.length < 2) throw error("openmodslib.command.calc_no_name");
			final String name = args[1];
			try {
				state.loadCalculator(name);
			} catch (NoSuchNameException e) {
				error("openmodslib.command.calc_invalid_name");
			}
		} else if (subCommand.equalsIgnoreCase(COMMAND_GET)) {
			if (args.length < 2) throw error("openmodslib.command.calc_no_key");
			final String key = args[1];
			try {
				state.getActiveProperty(key);
			} catch (NoSuchPropertyException e) {
				error("openmodslib.command.calc_invalid_key");
			}
		} else if (subCommand.equalsIgnoreCase(COMMAND_SET)) {
			if (args.length < 2) throw error("openmodslib.command.calc_no_key");
			if (args.length < 3) throw error("openmodslib.command.calc_no_value");
			final String key = args[1];
			final String value = args[2];

			try {
				state.setActiveProperty(key, value);
			} catch (NoSuchPropertyException e) {
				error("openmodslib.command.calc_invalid_key");
			} catch (Exception e) {
				error("openmodslib.command.calc_cant_set");
			}
		} else if (subCommand.equalsIgnoreCase(COMMAND_MODE)) {
			final String type = args[1].toUpperCase(Locale.ENGLISH);
			try {
				state.exprType = ExprType.valueOf(type);
			} catch (IllegalArgumentException e) {
				throw error("openmodslib.command.calc_invalid_mode", Joiner.on(',').join(ExprType.values()));
			}
		} else {
			throw new SyntaxErrorException();
		}
	}

	@Override
	public List<?> addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 0) return null;
		if (args.length == 1) return filterPrefixes(args[0], SUB_COMMANDS);

		final String subCommand = args[0];

		if (subCommand.equalsIgnoreCase(COMMAND_NEW)) {
			final Object[] values = CalculatorType.values();
			final Iterable<String> types = stringifyList(values);
			return filterPrefixes(args[1], types);
		} else if (subCommand.equalsIgnoreCase(COMMAND_MODE)) {
			final Object[] values = ExprType.values();
			final Iterable<String> types = stringifyList(values);
			return filterPrefixes(args[1], types);
		} else if (subCommand.equalsIgnoreCase(COMMAND_LOAD)) {
			return filterPrefixes(args[1], state.getCalculatorsNames());
		} else if (subCommand.equalsIgnoreCase(COMMAND_GET) || subCommand.equalsIgnoreCase(COMMAND_SET)) {
			return filterPrefixes(args[1], state.getActiveProperties());
		} else return null;
	}

	private static Iterable<String> stringifyList(Object... values) {
		return Iterables.transform(Arrays.asList(values), new Function<Object, String>() {
			@Override
			@Nullable
			public String apply(@Nullable Object input) {
				return String.valueOf(input).toLowerCase(Locale.ENGLISH);
			}
		});
	}
}
