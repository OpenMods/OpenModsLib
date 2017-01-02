package openmods.calc.parsing;

import openmods.calc.IExecutable;

public interface IPostfixCompilerState<E> {

	public enum Result {
		ACCEPTED,
		ACCEPTED_AND_FINISHED,
		REJECTED;
	}

	public Result acceptToken(Token token);

	public Result acceptExecutable(IExecutable<E> executable);

	public IExecutable<E> exit();

}
