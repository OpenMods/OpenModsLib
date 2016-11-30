package openmods.calc;

public abstract class NullaryFunction<E> extends FixedCallable<E> {

	public NullaryFunction() {
		super(0, 1);
	}

	protected abstract E call();

	@Override
	public final void call(Frame<E> frame) {
		frame.stack().push(call());
	}

}
