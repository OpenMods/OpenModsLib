package openmods.calc.parsing.postfix;

import com.google.common.base.Preconditions;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.parsing.token.TokenUtils;

public abstract class BracketPostfixParserStateBase<E> extends SimplePostfixParserState<E> {
	private final String openingBracket;
	private boolean isFinished;

	public BracketPostfixParserStateBase(IExecutableListBuilder<E> builder, String openingBracket) {
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
	public E getResult() {
		Preconditions.checkState(isFinished, "Missing closing bracket");
		final E compiledExpr = super.getResult();
		return processCompiledBracket(compiledExpr);
	}

	protected abstract E processCompiledBracket(E compiledExpr);
}