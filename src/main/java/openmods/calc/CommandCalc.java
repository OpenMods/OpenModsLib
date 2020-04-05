package openmods.calc;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import info.openmods.calc.ExprType;
import java.util.Locale;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import openmods.config.simpler.ConfigurableClassAdapter;
import openmods.utils.StackUnderflowException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandCalc {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final DynamicCommandExceptionType EXECUTE_EXCEPTION = new DynamicCommandExceptionType((arg) -> new TranslationTextComponent("openmodslib.command.calc_error", arg));
	private static final SimpleCommandExceptionType INVALID_NAME_EXCEPTION = new SimpleCommandExceptionType(new TranslationTextComponent("openmodslib.command.calc_invalid_name"));
	private static final SimpleCommandExceptionType STACK_UNDERFLOW_EXCEPTION = new SimpleCommandExceptionType(new TranslationTextComponent("openmodslib.command.calc_stack_underflow"));
	private static final SimpleCommandExceptionType INVALID_PROPERTY_KEY_EXCEPTION = new SimpleCommandExceptionType(new TranslationTextComponent("openmodslib.command.calc_invalid_key"));
	private static final SimpleCommandExceptionType CANT_SET_PROPERTY_EXCEPTION = new SimpleCommandExceptionType(new TranslationTextComponent("openmodslib.command.calc_cant_set"));

	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		final CalcState state = new CalcState();

		final LiteralCommandNode<CommandSource> eval = dispatcher.register(
				literal("=")
						.then(
								argument("expr", greedyString())
										.executes(context -> {
											final String expr = StringArgumentType.getString(context, "expr");
											final CommandSource sender = context.getSource();
											try {
												if (state.exprType.hasSingleResult) {
													final String result = state.compileExecuteAndPrint(sender, expr);
													sender.sendFeedback(new StringTextComponent(result), false);
												} else {
													state.compileAndExecute(sender, expr);
													sender.sendFeedback(new TranslationTextComponent("openmodslib.command.calc_stack_size", state.getActiveCalculator().environment.topFrame().stack().size()), false);
												}
											} catch (final Exception e) {
												LOGGER.error("Failed to run calc expressions");
												throw EXECUTE_EXCEPTION.create(e.getMessage());
											}
											// TODO return value if applicable
											return 0;
										})
						)
		);

		dispatcher.register(
				literal("calc")
						.then(literal("eval")
								.redirect(eval))
						.then(
								literal("fun")
										.then(
												argument("name", word())
														.then(
																argument("arg_count", integer(0))
																		.then(
																				argument("body", greedyString())
																						.executes(context -> {
																							final String name = getString(context, "name");
																							final int argCount = getInteger(context, "arg_count");
																							final String body = getString(context, "body");

																							state.compileAndDefineGlobalFunction(context.getSource(), name, argCount, body);
																							context.getSource().sendFeedback(new TranslationTextComponent("openmodslib.command.calc_function_defined", name), false);
																							return 0;
																						})
																		)
														)
										)
						)
						.then(
								literal("let")
										.then(
												argument("name", word())
														.then(
																argument("initializer", greedyString())
																		.executes(context -> {
																			final String name = getString(context, "name");
																			final String expr = getString(context, "initializer");
																			final Object result;
																			try {
																				result = state.compileAndSetGlobalSymbol(context.getSource(), name, expr);
																			} catch (final Exception e) {
																				LOGGER.error("Failed to run calc expressions");
																				throw EXECUTE_EXCEPTION.create(e.getMessage());
																			}

																			context.getSource().sendFeedback(new TranslationTextComponent("openmodslib.command.calc_set", name, String.valueOf(result)), false);
																			// TODO better return value if possible
																			return result instanceof Number? ((Number)result).intValue() : 0;
																		})
														)
										)
						)
		);

		final LiteralArgumentBuilder<CommandSource> configLiteral = literal("calc")
				.then(
						literal("config")
				);

		configLiteral
				.then(
						literal("load")
								.then(
										argument("name", word())
												.executes(context -> {
															try {
																state.loadCalculator(getString(context, "name"));
															} catch (CalcState.NoSuchNameException e) {
																throw INVALID_NAME_EXCEPTION.create();
															}
															return 0;
														}
												)

								)
				)
				.then(
						literal("store")
								.then(
										argument("name", word())
												.executes(context -> {
													state.nameCalculator(getString(context, "name"));
													return 0;
												})
								)

				)
				.then(
						literal("pop")
								.executes(context -> {
									final int size = state.pushCalculator();
									context.getSource().sendFeedback(new TranslationTextComponent("openmodslib.command.calc_after_push", size), false);
									return size;
								})
				)
				.then(
						literal("push")
								.executes(context -> {
									try {
										final int size = state.popCalculator();
										context.getSource().sendFeedback(new TranslationTextComponent("openmodslib.command.calc_after_pop", size), false);
										return size;
									} catch (StackUnderflowException e) {
										throw STACK_UNDERFLOW_EXCEPTION.create();
									}
								})
				)
				.then(
						literal("set")
								.then(
										argument("key", word())
												.suggests((context, builder) -> ISuggestionProvider.suggest(state.getActiveCalculator().getProperties(), builder))
												.then(
														argument("value", greedyString())
																.executes(context -> {
																	final String key = getString(context, "key");
																	final String value = getString(context, "value");
																	try {
																		state.getActiveCalculator().setProperty(key, value);
																	} catch (ConfigurableClassAdapter.NoSuchPropertyException e) {
																		throw INVALID_PROPERTY_KEY_EXCEPTION.create();
																	} catch (Exception e) {
																		LOGGER.error("Failed to set property {} to value {}", key, value, e);
																		throw CANT_SET_PROPERTY_EXCEPTION.create();
																	}

																	return 0;
																})
												)

								)
				)
				.then(
						literal("get")
								.then(
										argument("key", word())
												.suggests((context, builder) -> ISuggestionProvider.suggest(state.getActiveCalculator().getProperties(), builder))
												.executes(context -> {
													final String key = getString(context, "key");
													try {
														context.getSource().sendFeedback(new StringTextComponent(state.getActiveCalculator().getProperty(key)), false);
													} catch (ConfigurableClassAdapter.NoSuchPropertyException e) {
														throw INVALID_PROPERTY_KEY_EXCEPTION.create();
													}
													return 0;
												})
								)
				);

		final LiteralArgumentBuilder<CommandSource> newLiteral = configLiteral.then(
				literal("new")
		);

		for (
				final CalcState.CalculatorType type : CalcState.CalculatorType.values()) {
			newLiteral.then(
					literal(type.name().toLowerCase(Locale.ROOT))
							.executes(context -> {
								state.createCalculator(type);
								return 0;
							})
			);
		}

		final LiteralArgumentBuilder<CommandSource> modeLiteral = configLiteral.then(
				literal("mode")
		);

		for (
				final ExprType type : ExprType.values()) {
			modeLiteral.then(
					literal(type.name().toLowerCase(Locale.ROOT))
							.executes(context -> {
								state.exprType = type;
								return 0;
							})
			);
		}

	}

}
