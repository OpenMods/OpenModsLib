package openmods.calc;

public interface IValuePrinter<E> {
	public String str(E value);

	public String repr(E value);
}
