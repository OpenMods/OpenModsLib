package openmods.utils;

public class StackValidationException extends RuntimeException {
	private static final long serialVersionUID = -6473451138801686555L;

	public StackValidationException(String message) {
		super(message);
	}

	public StackValidationException(String format, Object... args) {
		super(String.format(format, args));
	}
}