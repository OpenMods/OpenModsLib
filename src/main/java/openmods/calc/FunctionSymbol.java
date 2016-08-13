package openmods.calc;

public abstract class FunctionSymbol<E> implements ISymbol<E> {

	@Override
	public void get(ICalculatorFrame<E> frame) {
		throw new UnsupportedOperationException("Can't reference function as value");
	}

}
