package openmods.calc;

public abstract class Operator<E> implements IExecutable<E> {

	public abstract boolean isLessThan(Operator<E> other);

}
