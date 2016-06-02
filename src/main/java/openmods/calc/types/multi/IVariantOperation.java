package openmods.calc.types.multi;

public interface IVariantOperation<L, R> {
	public TypedValue apply(TypeDomain domain, L left, R right);
}