package openmods.calc.types.multi;

public interface IComposite {

	public TypedValue get(TypeDomain domain, String component);

	public String subtype();

}
