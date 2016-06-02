package openmods.calc.types.multi;

public interface IConverter<S, T> {
	public T convert(S value);
}