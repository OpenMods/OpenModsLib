package openmods.calc;

public interface IStackSymbol<E> extends ISymbol<E> {
	public void checkArgumentCount(int argCount);
}
