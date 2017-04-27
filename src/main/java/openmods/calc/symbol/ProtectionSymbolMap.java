package openmods.calc.symbol;

public class ProtectionSymbolMap<E> extends NestedSymbolMap<E> {

	public ProtectionSymbolMap(SymbolMap<E> parent) {
		super(parent);
	}

	@Override
	public void put(String name, ISymbol<E> symbol) {
		throw new UnsupportedOperationException("Tried to set symbol " + name + " on read-only frame");
	}

}
