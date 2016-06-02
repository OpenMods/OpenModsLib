package openmods.calc.types.multi;

public interface ICoercedOperation<T> {
	public T apply(TypeDomain domain, T left, T right);
}