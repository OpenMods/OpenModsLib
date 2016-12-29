package openmods.calc.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;

public class CommandExecutionException extends RuntimeException {
	private static final long serialVersionUID = -2804083418573424384L;

	private List<String> path = Lists.newArrayList();

	public CommandExecutionException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		return "Failed to execute " + Joiner.on("::").join(Lists.reverse(path));
	}

	public CommandExecutionException pushCommandName(String name) {
		path.add(name);
		return this;
	}

}
