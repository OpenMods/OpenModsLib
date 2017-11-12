package openmods.calc;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import info.openmods.calc.ExprType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;
import openmods.calc.CalcState.CalculatorType;
import openmods.calc.CalcState.NoSuchNameException;
import openmods.config.simpler.ConfigurableClassAdapter.NoSuchPropertyException;
import openmods.utils.CommandUtils;
import openmods.utils.StackUnderflowException;

public class CommandCalcFactory {

	private final File scriptDir;
	private final CalcState state = new CalcState();

	public CommandCalcFactory(File scriptDir) {
		this.scriptDir = scriptDir.getAbsoluteFile();
	}

	private final ICommandComponent root = MapCommandComponent.builder()
			.put("config",
					MapCommandComponent.builder()
							.put("new", new TerminalCommandComponent(Arrays.toString(CalculatorType.values())) {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									final String type = args.getNextPart().toUpperCase(Locale.ROOT);
									try {
										final CalculatorType newType = CalculatorType.valueOf(type);
										state.createCalculator(newType);
									} catch (IllegalArgumentException e) {
										throw new CommandSyntaxException("openmodslib.command.calc_invalid_type", Joiner.on(',').join(CalculatorType.values()));
									}
								}

								@Override
								public List<String> getTabCompletions(IWhitespaceSplitter args) {
									final String type = args.getNextPart();
									final Object[] values = CalculatorType.values();
									final Iterable<String> types = stringifyList(values);
									return CommandUtils.filterPrefixes(type, types);
								}
							})
							.put("load", new TerminalCommandComponent("<name>") {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									try {
										state.loadCalculator(args.getNextPart());
									} catch (NoSuchNameException e) {
										throw new CommandSyntaxException("openmodslib.command.calc_invalid_name");
									}
								}

								@Override
								public List<String> getTabCompletions(IWhitespaceSplitter args) {
									final String name = args.getNextPart();
									return CommandUtils.filterPrefixes(name, state.getCalculatorsNames());
								}
							})
							.put("store", new TerminalCommandComponent("<name>") {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									state.nameCalculator(args.getNextPart());
								}

							})
							.put("pop", new TerminalCommandComponent("") {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									final int size = state.pushCalculator();
									CommandUtils.respond(sender, "openmodslib.command.calc_after_push", size);
								}
							})
							.put("push", new TerminalCommandComponent("") {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									try {
										final int size = state.popCalculator();
										CommandUtils.respond(sender, "openmodslib.command.calc_after_pop", size);
									} catch (StackUnderflowException e) {
										throw new CommandSyntaxException("openmodslib.command.calc_stack_underflow");
									}
								}
							})
							.put("set", new TerminalCommandComponent("<key> <value>") {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									final String key = args.getNextPart();
									final String value = args.getTail();

									try {
										state.getActiveCalculator().setProperty(key, value);
									} catch (NoSuchPropertyException e) {
										throw new CommandSyntaxException("openmodslib.command.calc_invalid_key");
									} catch (Exception e) {
										throw new CommandSyntaxException("openmodslib.command.calc_cant_set");
									}
								}

								@Override
								public List<String> getTabCompletions(IWhitespaceSplitter args) {
									final String key = args.getNextPart();
									if (!args.isFinished()) return Lists.newArrayList();
									return CommandUtils.filterPrefixes(key, state.getActiveCalculator().getProperties());
								}

							})
							.put("get", new TerminalCommandComponent("<key>") {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									final String key = args.getNextPart();
									try {
										state.getActiveCalculator().getProperty(key);
									} catch (NoSuchPropertyException e) {
										throw new CommandSyntaxException("openmodslib.command.calc_invalid_key");
									}
								}

								@Override
								public List<String> getTabCompletions(IWhitespaceSplitter args) {
									final String key = args.getNextPart();
									return CommandUtils.filterPrefixes(key, state.getActiveCalculator().getProperties());
								}

							})
							.put("mode", new TerminalCommandComponent(Arrays.toString(ExprType.values())) {
								@Override
								public void execute(ICommandSender sender, IWhitespaceSplitter args) {
									final String type = args.getNextPart().toUpperCase(Locale.ROOT);
									try {
										state.exprType = ExprType.valueOf(type);
									} catch (IllegalArgumentException e) {
										throw new CommandSyntaxException("openmodslib.command.calc_invalid_mode", Joiner.on(',').join(ExprType.values()));
									}
								}
							})
							.build())
			.put("execute", new TerminalCommandComponent("<path>") {
				@Override
				public void execute(ICommandSender sender, IWhitespaceSplitter args) {
					final String path = args.getNextPart();
					final File scriptFile = new File(scriptDir, path).getAbsoluteFile();
					if (!checkIsParent(scriptDir, scriptFile))
						throw new CommandExecutionException("openmodslib.command.calc_not_child", scriptFile, scriptDir);
					if (!scriptFile.isFile())
						throw new CommandExecutionException("openmodslib.command.calc_not_file", scriptFile);
					final int count = executeScript(sender, scriptFile);
					CommandUtils.respond(sender, "openmodslib.command.calc_executed_count", count);
				}

				@Override
				public List<String> getTabCompletions(IWhitespaceSplitter args) {
					final String path = args.getNextPart().replace("\\", "/");
					final File scriptFile = new File(scriptDir, path).getAbsoluteFile();

					final File fileToScan = path.isEmpty() || path.endsWith("/")? scriptFile : scriptFile.getParentFile();
					if (!fileToScan.isDirectory()) return null;
					final int parentLengthPath = scriptDir.getAbsolutePath().length() + 1; // adding / on end
					final List<String> propositions = Lists.newArrayList();
					for (File child : fileToScan.listFiles()) {
						if (child.isFile())
							propositions.add(child.getAbsolutePath().substring(parentLengthPath).replace("\\", "/"));
						else if (child.isDirectory()) {
							propositions.add(child.getAbsolutePath().substring(parentLengthPath).replace("\\", "/") + "/");
						}
					}

					return CommandUtils.filterPrefixes(path, propositions);
				}

			})
			.put("let", new TerminalCommandComponent("<name> <initializer expression>") {
				@Override
				public void execute(ICommandSender sender, IWhitespaceSplitter args) {
					final String name = args.getNextPart();
					final String expr = args.getTail();

					final Object result = state.compileAndSetGlobalSymbol(sender, name, expr);
					CommandUtils.respond(sender, "openmodslib.command.calc_set", name, String.valueOf(result));
				}
			})
			.put("fun", new TerminalCommandComponent("<name> <arg count> <function body>") {
				@Override
				public void execute(ICommandSender sender, IWhitespaceSplitter args) {
					final String name = args.getNextPart();

					final String argCountStr = args.getNextPart();
					final int argCount;
					try {
						argCount = Integer.parseInt(argCountStr);
					} catch (NumberFormatException e) {
						throw new CommandSyntaxException("openmodslib.command.calc_invalid_number", argCountStr);
					}

					final String expr = args.getTail();
					state.compileAndDefineGlobalFunction(sender, name, argCount, expr);
					CommandUtils.respond(sender, "openmodslib.command.calc_function_defined", name);
				}
			})
			.put("eval", new TerminalCommandComponent("<expression>") {
				@Override
				public void execute(ICommandSender sender, IWhitespaceSplitter args) {
					final String expr = args.getTail();
					if (state.exprType.hasSingleResult) {
						final String result = state.compileExecuteAndPrint(sender, expr);
						CommandUtils.respondText(sender, result);
					} else {
						state.compileAndExecute(sender, expr);
						CommandUtils.respond(sender, "openmodslib.command.calc_stack_size", state.getActiveCalculator().environment.topFrame().stack().size());
					}
				}
			})
			.put("echo", new TerminalCommandComponent("<str>") {
				@Override
				public void execute(ICommandSender sender, IWhitespaceSplitter args) {
					sender.sendMessage(new TextComponentString(args.getTail()));
				}
			})
			.build();

	public ICommandComponent getRoot() {
		return root;
	}

	private int executeScript(ICommandSender sender, File scriptFile) {
		int count = 0;
		try {
			final Closer closer = Closer.create();
			try {
				final Reader r = closer.register(new FileReader(scriptFile));
				final BufferedReader br = closer.register(new BufferedReader(r));

				String line;
				while ((line = br.readLine()) != null) {
					final IWhitespaceSplitter args = WhitespaceSplitters.fromString(line);
					root.execute(sender, args);
					count++;
				}

			} finally {
				closer.close();
			}

			return count;
		} catch (Exception e) {
			throw new CommandExecutionException(e);
		}
	}

	private static boolean checkIsParent(File dir, File target) {
		if (!dir.exists()) return false;
		try {
			final File canonicalDir = dir.getCanonicalFile();
			while (target != null) {
				if (canonicalDir.equals(target)) return true;
				target = target.getParentFile();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return false;
	}

	private static Iterable<String> stringifyList(Object... values) {
		return Iterables.transform(Arrays.asList(values), new Function<Object, String>() {
			@Override
			@Nullable
			public String apply(@Nullable Object input) {
				return String.valueOf(input).toLowerCase(Locale.ROOT);
			}
		});
	}
}
