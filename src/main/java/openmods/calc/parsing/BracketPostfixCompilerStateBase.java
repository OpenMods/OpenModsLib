package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import openmods.calc.IExecutable;

public abstract class BracketPostfixCompilerStateBase<E> extends SimplePostfixCompilerState<E> {
	private final String openingBracket;
	private boolean isFinished;

	public BracketPostfixCompilerStateBase(IExecutableListBuilder<E> builder, String openingBracket) {
		super(builder);
		this.openingBracket = openingBracket;
	}

	@Override
	public Result acceptToken(Token token) {
		if (token.type == TokenType.RIGHT_BRACKET) {
			TokenUtils.checkIsValidBracketPair(openingBracket, token.value);
			isFinished = true;
			return Result.ACCEPTED_AND_FINISHED;
		}
		return processBracketContent(token);
	}

	protected Result processBracketContent(Token token) {
		return super.acceptToken(token);
	}

	@Override
	public IExecutable<E> exit() {
		Preconditions.checkState(isFinished, "Missing closing bracket");
		final IExecutable<E> compiledExpr = super.exit();
		return processCompiledBracket(compiledExpr);
	}

	protected abstract IExecutable<E> processCompiledBracket(IExecutable<E> compiledExpr);
}