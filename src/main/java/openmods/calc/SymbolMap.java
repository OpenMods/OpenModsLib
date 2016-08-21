package openmods.calc;

public abstract class SymbolMap<E> {

	protected abstract ISymbol<E> createSymbol(ICallable<E> callable);

	protected abstract ISymbol<E> createSymbol(IGettable<E> gettable);

	protected abstract ISymbol<E> createSymbol(E value);

	public abstract void put(String name, ISymbol<E> symbol);

	public void put(String name, ICallable<E> callable) {
		put(name, createSymbol(callable));
	}

	public void put(String name, IGettable<E> gettable) {
		put(name, createSymbol(gettable));
	}

	public void put(String name, E value) {
		put(name, createSymbol(value));
	}

	public abstract ISymbol<E> get(String name);
}
