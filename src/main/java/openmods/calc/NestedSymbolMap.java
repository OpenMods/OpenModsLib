package openmods.calc;

public abstract class NestedSymbolMap<E> extends SymbolMap<E> {

	private final SymbolMap<E> parent;

	public NestedSymbolMap(SymbolMap<E> parent) {
		this.parent = parent;
	}

	@Override
	protected ISymbol<E> createSymbol(ICallable<E> callable) {
		return parent.createSymbol(callable);
	}

	@Override
	protected ISymbol<E> createSymbol(IGettable<E> gettable) {
		return parent.createSymbol(gettable);
	}

	@Override
	protected ISymbol<E> createSymbol(E value) {
		return parent.createSymbol(value);
	}

	@Override
	public ISymbol<E> get(String name) {
		return parent.get(name);
	}

}
