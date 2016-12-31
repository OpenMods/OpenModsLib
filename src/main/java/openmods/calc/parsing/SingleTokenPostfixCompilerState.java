package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import openmods.calc.IExecutable;

public abstract class SingleTokenPostfixCompilerState<E> implements IPostfixCompilerState<E> {
	private static class RejectToken extends RuntimeException {
		private static final long serialVersionUID = 4788201874529404099L;

	}

	private IExecutable<E> result;

	protected abstract IExecutable<E> parseToken(Token token);

	protected IExecutable<E> rejectToken() {
		throw new RejectToken();
	}

	@Override
	public Result acceptToken(Token token) {
		Preconditions.checkState(result == null);

		try {
			result = parseToken(token);
			return Result.ACCEPTED_AND_FINISHED;
		} catch (RejectToken e) {
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