package openmods.source;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.TranslationTextComponent;
import openmods.Log;
import openmods.source.ClassSourceCollector.ClassMeta;

public class SourceCommand {

	private static final DynamicCommandExceptionType CLASS_NOT_FOUND = new DynamicCommandExceptionType(o -> new TranslationTextComponent("openmodslib.command.invalid_class_name", o));

	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		final ClassSourceCollector collector = new ClassSourceCollector();

		dispatcher.register(
				literal("source")
						.then(
								literal("class")
										.then(
												argument("cls", string())
														.executes(context -> {
															final ClassMeta meta = getMeta(collector, getString(context, "cls"));
															final CommandSource source = context.getSource();
															source.sendFeedback(new TranslationTextComponent("openmodslib.command.class_source", meta.cls.getName(), meta.source()), false);

															// TODO 1.14 Rest of it, if present
															return 1;
														})
										)
						)
		);
	}

	private static ClassMeta getMeta(final ClassSourceCollector classMeta, final String clsName) throws CommandSyntaxException {
		try {
			return classMeta.getClassInfo(clsName);
		} catch (ClassNotFoundException e) {
			Log.warn(e, "Failed to get information for class %s", clsName);
			throw CLASS_NOT_FOUND.create(clsName);
		} catch (Throwable t) {
			Log.warn(t, "Failed to get information for class %s", clsName);
			throw t;
		}
	}

}
