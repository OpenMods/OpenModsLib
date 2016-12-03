package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.ISymbol;
import openmods.calc.SymbolMap;

public class EnvMap extends SimpleComposite implements CompositeTraits.Structured {

	private final SymbolMap<TypedValue> symbols;

	public EnvMap(SymbolMap<TypedValue> symbols) {
		this.symbols = symbols;
	}

	@Override
	public Optional<TypedValue> get(TypeDomain domain, String component) {
		final ISymbol<TypedValue> result = symbols.get(component);
		if (result == null) return Optional.absent();
		return Optional.of(result.get());
	}

	@Override
	public String type() {
		return "env";
	}

}
