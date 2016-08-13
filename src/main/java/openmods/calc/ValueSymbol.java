package openmods.calc;

public abstract class ValueSymbol<E> extends FixedSymbol<E> {

	public ValueSymbol() {
		super(0, 1);
	}

	@Override
	public final void call(ICalculatorFrame<E> frame) {
		get(frame);
	}
}
