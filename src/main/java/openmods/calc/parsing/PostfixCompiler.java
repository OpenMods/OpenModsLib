package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.PeekingIterator;
import openmods.calc.IExecutable;
import openmods.calc.parsing.IPostfixCompilerState.Result;
import openmods.utils.Stack;

public abstract class PostfixCompiler<E> implements ITokenStreamCompiler<E> {

	@Override
	public IExecutable<E> compile(PeekingIterator<Token> input) {
		final Stack<IPostfixCompilerState<E>> stateStack = Stack.create();
		stateStack.push(createInitialState());

		while (input.hasNext()) {
			final Token token = input.next();
			if (token.type == TokenType.MODIFIER) {
				stateStack.push(createStateForModifier(token.value));
			} else if (token.type == TokenType.LEFT_BRACKET) {
				stateStack.push(createStateForBracket(token.value));
			} else {
				final IPostfixCompilerState<E> currentState = stateStack.peek(0);
				final Result result = currentState.acceptToken(token);
				switch (result) {
					case ACCEPTED_AND_FINISHED:
						unwindStack(stateStack);
						// fall-through
					case ACCEPTED:
						// NO-OP
						break;
					case REJECTED:
					default:
						throw new IllegalStateException("Token  " + token + " not accepted in state " + currentState);
				}
			}
		}

		Preconditions.checkState(stateStack.size() == 1, "Invalid compiler stack state, got %s entries", stateStack.size());
		final IPostfixCompilerState<E> finalState = stateStack.pop();
		return finalState.exit();
	}

	private void unwindStack(Stack<IPostfixCompilerState<E>> stateStack) {
		IPostfixCompilerState<E> currentState = stateStack.pop();
		UNWIND: while (true) {
			final IExecutable<E> exitResult = currentState.exit();
			currentState = stateStack.peek(0);
			final Result acceptResult = currentState.acceptExecutable(exitResult);
			switch (acceptResult) {
				case ACCEPTED_AND_FINISHED:
					stateStack.pop();
					continue UNWIND;
				case ACCEPTED:
					break UNWIND;
				case REJECTED:
				default:
					throw new IllegalStateException("Executable  " + exitResult + " not accepted in state " + currentState);
			}
		}
	}

	protected abstract IPostfixCompilerState<E> createInitialState();

	protected IPostfixCompilerState<E> createStateForModifier(String modifier) {
		throw new UnsupportedOperationException(modifier);
	}

	protected IPostfixCompilerState<E> createStateForBracket(String modifier) {
		throw new UnsupportedOperationException(modifier);
	}
}
