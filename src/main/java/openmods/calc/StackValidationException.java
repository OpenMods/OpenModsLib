package openmods.calc;

public class StackValidationException extends RuntimeException {
	private static final long serialVersionUID = -6473451138801686555L;

	public StackValidationException(String message, Object... args) {
		super(String.format(message, args));
	}
}