package openmods.calc.parsing;

import com.google.common.base.Optional;
import openmods.calc.IExecutable;

public interface IExecutableListBuilder<E> {

	public void appendValue(E value);

	public void appendValue(Token value);

	public void appendOperator(String id);

	public void appendSymbolGet(String id);

	public void appendSymbolCall(String id, Optional<Integer> argCount, Optional<Integer> returnCount);

	public void appendExecutable(IExecutable<E> executable);

	public IExecutable<E> build();
}
