package openmods.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import openmods.utils.CommandUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.opengl.KHRDebugCallback;

public class CommandGlDebug extends CommandBase {

	@Override
	public String getName() {
		return "gl_debug";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "gl_debug high|medium|low|notification|disable";
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	private static final List<String> STATES = ImmutableList.of("high", "medium", "low", "notification", "disable");

	private static final Set<Integer> ALL_LEVELS = ImmutableSet.of(KHRDebug.GL_DEBUG_SEVERITY_HIGH, KHRDebug.GL_DEBUG_SEVERITY_MEDIUM, KHRDebug.GL_DEBUG_SEVERITY_LOW, KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION);

	private static final Map<String, ? extends Set<Integer>> ALLOWED_LEVELS = ImmutableMap.of(
			"notification", ImmutableSet.of(KHRDebug.GL_DEBUG_SEVERITY_HIGH, KHRDebug.GL_DEBUG_SEVERITY_MEDIUM, KHRDebug.GL_DEBUG_SEVERITY_LOW, KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION),
			"low", ImmutableSet.of(KHRDebug.GL_DEBUG_SEVERITY_HIGH, KHRDebug.GL_DEBUG_SEVERITY_MEDIUM, KHRDebug.GL_DEBUG_SEVERITY_LOW),
			"medium", ImmutableSet.of(KHRDebug.GL_DEBUG_SEVERITY_HIGH, KHRDebug.GL_DEBUG_SEVERITY_MEDIUM),
			"high", ImmutableSet.of(KHRDebug.GL_DEBUG_SEVERITY_HIGH));

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		if (args.length == 1) {
			String state = args[0];
			return CommandUtils.filterPrefixes(state, STATES);
		}
		return Collections.emptyList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 1) throw new CommandException("commands.generic.syntax");
		final String arg = args[0].toLowerCase(Locale.ROOT);
		if (arg.equals("disable")) {
			GL11.glDisable(KHRDebug.GL_DEBUG_OUTPUT);
		} else {
			final Set<Integer> allowedLevels = ALLOWED_LEVELS.get(arg);
			if (allowedLevels == null) throw new CommandException("commands.generic.syntax");

			GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT);
			for (int level : ALL_LEVELS) {
				final boolean isEnabled = allowedLevels.contains(level);
				KHRDebug.glDebugMessageControl(GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, level, null, isEnabled);
			}
			KHRDebug.glDebugMessageCallback(new KHRDebugCallback());
		}

	}

}
