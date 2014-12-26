package openmods.context;

public class VariableKey<T> {

	private VariableKey() {}

	public static <T> VariableKey<T> create() {
		return new VariableKey<T>();
	}
}
