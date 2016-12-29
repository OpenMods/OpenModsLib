package openmods.calc.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

public class CommandSyntaxException extends RuntimeException {
	private static final long serialVersionUID = -781052257944634757L;

	private List<String> path = Lists.newArrayList();

	private String message;
	private Object[] args;

	public CommandSyntaxException(String message, Object... args) {
		this.message = message;
		this.args = args;
	}

	public CommandSyntaxException pushCommandName(String name) {
		path.add(name);
		return this;
	}

	public IChatComponent getChatComponent() {
		return new ChatComponentTranslation("openmodslib.command.calc_error_path", Joiner.on("::").join(Lists.reverse(path)))
				.appendSibling(new ChatComponentTranslation(message, args));
	}
}
