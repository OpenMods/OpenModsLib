package openmods.calc.parsing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import openmods.calc.IExecutable;

public abstract class SingleTokenPostfixCompilerState<E> implements IPostfixCompilerState<E> {
	private IExecutable<E> result;

	protected abstract Optional<? extends IExecutable<E>> parseToken(Token token);

	@Override
	public Result acceptToken(Token token) {
		Preconditions.checkState(result == null);

		final Optional<? extends IExecutable<E>> parseResult = parseToken(token);

		if (parseResult.isPresent()) {
			result = parseResult.get();
			return Result.ACCEPTED_AND_FINISHED;
		} else {
			return Result.REJECTED;
		}
	}

	@Override
	public Result acceptExecutable(IExecutable<E> executable) {
		return Result.REJECTED;
	}

	@Override
	public IExecutable<E> exit() {
		Preconditions.checkState(result != null);
		return result;
	}
}