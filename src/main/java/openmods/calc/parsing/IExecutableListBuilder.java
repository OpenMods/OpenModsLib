package openmods.calc.parsing;

import openmods.calc.IExecutable;
import openmods.utils.OptionalInt;

public interface IExecutableListBuilder<E> {

	public void appendValue(E value);

	public void appendValue(Token value);

	public void appendOperator(String id);

	public void appendSymbolGet(String id);

	public void appendSymbolCall(String id, OptionalInt argCount, OptionalInt returnCount);

	public void appendExecutable(IExecutable<E> executable);

	public IExecutable<E> build();
}
