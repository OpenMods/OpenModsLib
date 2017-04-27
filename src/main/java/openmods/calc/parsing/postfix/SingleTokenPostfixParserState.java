package openmods.calc.parsing.postfix;

import com.google.common.base.Preconditions;
import openmods.calc.parsing.token.Token;

public abstract class SingleTokenPostfixParserState<E> implements IPostfixParserState<E> {
	private static class RejectToken extends RuntimeException {
		private static final long serialVersionUID = 4788201874529404099L;
	}

	private E result;

	protected abstract E parseToken(Token token);

	protected E rejectToken() {
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
	public Result acceptChildResult(E executable) {
		return Result.REJECTED;
	}

	@Override
	public E getResult() {
		Preconditions.checkState(result != null);
		return result;
	}
}