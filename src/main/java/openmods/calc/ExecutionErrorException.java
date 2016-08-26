package openmods.calc;

public class ExecutionErrorException extends RuntimeException {
	private static final long serialVersionUID = -2758372139636343355L;

	public ExecutionErrorException() {
		super();
	}

	public ExecutionErrorException(String cause) {
		super(cause);
	}

}
