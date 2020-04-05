package openmods.config.properties;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;

public class CommandConfig {

	// TODO 1.14 Everything, if still makes sense
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
				literal("om_config")
						.then(
								literal("save")
										.then(
												argument("modid", StringArgumentType.string())
														.executes(context -> 0)
										)
						)
						.then(
								literal("help")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.executes(context -> 0)
																		)
														)
										)
						)
						.then(
								literal("get")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.executes(context -> 0)
																		)
														)
										)
						)
						.then(
								literal("clear")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.executes(context -> 0)
																		)
														)
										)
						)
						.then(
								literal("default")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.executes(context -> 0)
																		)
														)
										)
						)
						.then(
								literal("set")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.then(
																								argument("value", StringArgumentType.greedyString())
																										.executes(context -> 0)
																						)

																		)
														)
										)
						)
						.then(
								literal("append")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.then(
																								argument("value", StringArgumentType.greedyString())
																										.executes(context -> 0)
																						)

																		)
														)
										)
						)
						.then(
								literal("remove")
										.then(
												argument("modid", StringArgumentType.string())
														.then(
																argument("category", StringArgumentType.string())
																		.then(
																				argument("name", StringArgumentType.string())
																						.then(
																								argument("value", StringArgumentType.greedyString())
																										.executes(context -> 0)
																						)

																		)
														)
										)
						)
		);
	}
}
