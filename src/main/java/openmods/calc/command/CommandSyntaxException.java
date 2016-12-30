package openmods.calc.command;

public class CommandSyntaxException extends NestedCommandException {
	private static final long serialVersionUID = -781052257944634757L;

	public CommandSyntaxException(String message, Object... args) {
		super(message, args);
	}

	@Override
	protected String contents() {
		return "openmodslib.command.calc_syntax_error_path";
	}
}