package openmods.calc;

public abstract class FixedFunctionSymbol<E> extends FixedSymbol<E> {

	public FixedFunctionSymbol(int argCount, int resultCount) {
		super(argCount, resultCount);
	}

	@Override
	public void get(ICalculatorFrame<E> frame) {
		throw new UnsupportedOperationException("Can't reference function as value");
	}

}
