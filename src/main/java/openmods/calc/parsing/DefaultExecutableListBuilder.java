package openmods.calc.parsing;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.executable.ExecutableList;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.NoopExecutable;
import openmods.calc.executable.Operator;
import openmods.calc.executable.SymbolCall;
import openmods.calc.executable.SymbolGet;
import openmods.calc.executable.Value;
import openmods.calc.parsing.ast.IOperatorDictionary;
import openmods.calc.parsing.ast.OperatorArity;
import openmods.calc.parsing.postfix.IExecutableListBuilder;
import openmods.calc.parsing.token.Token;
import openmods.utils.OptionalInt;

public class DefaultExecutableListBuilder<E> implements IExecutableListBuilder<IExecutable<E>> {

	private final IValueParser<E> valueParser;
	private final IOperatorDictionary<Operator<E>> operators;

	private final List<IExecutable<E>> buffer = Lists.newArrayList();

	public DefaultExecutableListBuilder(IValueParser<E> valueParser, IOperatorDictionary<Operator<E>> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	protected void addToBuffer(IExecutable<E> executable) {
		buffer.add(executable);
	}

	@Override
	public void appendValue(Token token) {
		try {
			final E value = valueParser.parseToken(token);
			addToBuffer(Value.create(value));
		} catch (Throwable t) {
			throw new InvalidTokenException(token, t);
		}
	}

	private Operator<E> getAnyOperator(String id) {
		Operator<E> op = operators.getOperator(id, OperatorArity.BINARY);
		if (op != null) return op;
		op = operators.getOperator(id, OperatorArity.UNARY);
		if (op != null) return op;
		throw new IllegalArgumentException("Invalid operator: " + id);
	}

	@Override
	public void appendOperator(String id) {
		addToBuffer(getAnyOperator(id));
	}

	@Override
	public void appendSymbolGet(String id) {
		addToBuffer(new SymbolGet<E>(id));
	}

	@Override
	public void appendSymbolCall(String id, OptionalInt argCount, OptionalInt returnCount) {
		addToBuffer(new SymbolCall<E>(id, argCount, returnCount));
	}

	@Override
	public void appendSubList(IExecutable<E> executable) {
		if (executable instanceof NoopExecutable) {
			// well, no-op
		} else if (executable instanceof ExecutableList) {
			List<IExecutable<E>> flattenedList = Lists.newArrayList();
			((ExecutableList<E>)executable).deepFlatten(flattenedList);

			for (IExecutable<E> e : flattenedList)
				addToBuffer(e);
		} else {
			addToBuffer(executable);
		}
	}

	@Override
	public IExecutable<E> build() {
		return ExecutableList.wrap(buffer);
	}

}
