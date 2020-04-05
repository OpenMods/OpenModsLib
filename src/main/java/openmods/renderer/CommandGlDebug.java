package openmods.renderer;

import static net.minecraft.command.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.renderer.GlDebugTextUtils;
import net.minecraft.command.CommandSource;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;

public class CommandGlDebug {
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		final LiteralArgumentBuilder<CommandSource> enable = literal("enable");

		createCallback(enable, "high", 0);
		createCallback(enable, "medium", 1);
		createCallback(enable, "low", 2);
		createCallback(enable, "notification", 3);

		dispatcher.register(
				literal("disable")
						.executes(context -> {
							GLCapabilities glcapabilities = GL.getCapabilities();
							if (glcapabilities.GL_KHR_debug) {
								// TODO 1.14 Free function?
								GL11.glDisable(KHRDebug.GL_DEBUG_OUTPUT);
							} // TODO 1.14 ARB?
							return 0;
						})
						.then(enable)
		);
	}

	private static void createCallback(LiteralArgumentBuilder<CommandSource> root, final String id, final int level) {
		root.then(
				literal(id)
						.executes(context -> {
							GlDebugTextUtils.setDebugVerbosity(level, false);
							return 1;
						})
						.then(
								literal("synchronous")
										.executes(context -> {
											GlDebugTextUtils.setDebugVerbosity(level, true);
											return 1;
										})
						)
		);
	}

}
