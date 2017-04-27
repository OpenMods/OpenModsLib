package openmods.calc.parsing.postfix;

import openmods.calc.parsing.token.Token;
import openmods.utils.OptionalInt;

public interface IExecutableListBuilder<E> {

	public void appendValue(Token value);

	public void appendOperator(String id);

	public void appendSymbolGet(String id);

	public void appendSymbolCall(String id, OptionalInt argCount, OptionalInt returnCount);

	public void appendSubList(E subList);

	public E build();
}
