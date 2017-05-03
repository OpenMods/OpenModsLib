package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;

public class CommandExecutionException extends NestedCommandException {
	private static final long serialVersionUID = -781052257944634757L;

	public CommandExecutionException(String message, Object... args) {
		super(message, args);
	}

	public CommandExecutionException(Throwable t) {
		super("openmodslib.command.calc_error", getThrowableCause(t));
	}

	private static String getThrowableCause(Throwable t) {
		final List<String> causes = Lists.newArrayList();
		Throwable current = t;
		while (current != null) {
			causes.add(Strings.nullToEmpty(current.getMessage()));
			current = current.getCause();
		}

		return Joiner.on("', caused by '").join(causes);
	}

	@Override
	protected String contents() {
		return "openmodslib.command.calc_runtime_error_path";
	}
}