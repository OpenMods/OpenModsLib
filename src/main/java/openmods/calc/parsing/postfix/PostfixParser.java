package openmods.calc.parsing.postfix;

import com.google.common.collect.PeekingIterator;
import openmods.calc.parsing.postfix.IPostfixParserState.Result;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.utils.Stack;

public abstract class PostfixParser<E> {

	public E parse(PeekingIterator<Token> input) {
		final Stack<IPostfixParserState<E>> stateStack = Stack.create();
		stateStack.push(createInitialState());

		while (input.hasNext()) {
			final Token token = input.next();
			if (token.type == TokenType.MODIFIER) {
				stateStack.push(createStateForModifier(token.value));
			} else if (token.type == TokenType.LEFT_BRACKET) {
				stateStack.push(createStateForBracket(token.value));
			} else {
				final IPostfixParserState<E> currentState = stateStack.peek(0);
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

		final IPostfixParserState<E> finalState = stateStack.popAndExpectEmptyStack();
		return finalState.getResult();
	}

	private void unwindStack(Stack<IPostfixParserState<E>> stateStack) {
		IPostfixParserState<E> currentState = stateStack.pop();
		UNWIND: while (true) {
			final E exitResult = currentState.getResult();
			currentState = stateStack.peek(0);
			final Result acceptResult = currentState.acceptChildResult(exitResult);
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

	protected abstract IPostfixParserState<E> createInitialState();

	protected IPostfixParserState<E> createStateForModifier(String modifier) {
		throw new UnsupportedOperationException(modifier);
	}

	protected IPostfixParserState<E> createStateForBracket(String bracket) {
		throw new UnsupportedOperationException(bracket);
	}
}
