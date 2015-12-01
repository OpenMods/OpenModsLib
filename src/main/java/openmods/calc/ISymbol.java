package openmods.calc;

public interface ISymbol<E> extends IExecutable<E> {
	public void checkArgumentCount(int argCount);

	public void checkResultCount(int resultCount);
}
