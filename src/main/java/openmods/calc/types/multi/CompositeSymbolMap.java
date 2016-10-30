package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.ISymbol;
import openmods.calc.NestedSymbolMap;
import openmods.calc.SymbolMap;

public class CompositeSymbolMap extends NestedSymbolMap<TypedValue> {

	private final TypeDomain domain;
	private final IComposite composite;

	public CompositeSymbolMap(SymbolMap<TypedValue> parent, TypeDomain domain, IComposite composite) {
		super(parent);
		this.domain = domain;
		this.composite = composite;
	}

	@Override
	public void put(String name, ISymbol<TypedValue> symbol) {
		throw new IllegalStateException("Trying to set value in read-only frame");
	}

	@Override
	public ISymbol<TypedValue> get(String name) {
		final Optional<TypedValue> result = composite.get(domain, name);
		return result.isPresent()
				? createSymbol(result.get())
				: super.get(name);
	}
}
