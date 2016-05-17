package openmods.calc.parsing;

import java.util.List;

import openmods.calc.*;
import openmods.utils.Stack;
import openmods.utils.Stack.StackUnderflowException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class InfixCompiler<E> implements ICompiler<E> {

	private class BracketMarker implements IExecutable<E> {
		private int argCount = 0;

		public int incrementArgCount() {
			return ++argCount;
		}

		@Override
		public void execute(ICalculatorFrame<E> frame) {
			throw new UnsupportedOperationException();
		}
	}

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	public InfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	@Override
	public IExecutable<E> compile(Iterable<Token> input) {
		final List<IExecutable<E>> output = Lists.newArrayList();
		final Stack<IExecutable<E>> operatorStack = Stack.create();

		Token lastToken = null;

		for (Token token : input) {
			if (token.type.isValue()) {
				final E value = valueParser.parseToken(token);
				output.add(Value.create(value));
			} else if (token.type.isPossibleFunction()) {
				Preconditions.checkArgument(token.type != TokenType.SYMBOL_WITH_ARGS, "Symbol '%s' can't be used in infix mode", token.value);
				operatorStack.push(new SymbolReference<E>(token.value));
			} else {
				if (lastToken != null && token.type != TokenType.LEFT_BRACKET && lastToken.type.isPossibleFunction()) {
					final IExecutable<E> top = operatorStack.pop();
					setArgCount(top, 0);
					output.add(top);
				}
				switch (token.type) {
					case LEFT_BRACKET:
						operatorStack.push(new BracketMarker());
						break;
					case RIGHT_BRACKET: {
						Preconditions.checkNotNull(lastToken, "Right bracket on invalid postion");
						final int argCount = lastToken.type != TokenType.LEFT_BRACKET? popUntilBracket(output, operatorStack) : 0;
						operatorStack.pop(); // left bracket
						if (!operatorStack.isEmpty()) {
							final IExecutable<E> top = operatorStack.peek(0);
							if (top instanceof SymbolReference) {
								((SymbolReference<?>)top).setArgumentsCount(argCount).setReturnsCount(1);
								operatorStack.pop();
								output.add(top);
							}
						} else {
							Preconditions.checkState(argCount > 0, "Empty brackets after non-fuction");
							Preconditions.checkState(argCount == 1, "Comma used in non-function brackets");
						}
						break;
					}
					case SEPARATOR: {
						popUntilBracket(output, operatorStack);
						break;
					}
					case OPERATOR: {
						final Operator<E> op;
						if (lastToken == null || lastToken.type.isNextOpUnary()) {
							op = operators.getUnaryOperator(token.value);
							Preconditions.checkArgument(op != null, "No unary version of operator: %s", token.value);
						} else
						{
							op = operators.getBinaryOperator(token.value);
							Preconditions.checkArgument(op != null, "Invalid operator: %s", token.value);
						}

						while (!operatorStack.isEmpty()) {
							final IExecutable<E> top = operatorStack.peek(0);
							if (!(top instanceof Operator)) break;

							final Operator<E> topOp = (Operator<E>)top;
							if (!op.isLessThan(topOp)) break;

							operatorStack.pop();
							output.add(top);
						}

						operatorStack.push(op);
						break;
					}
					default:
						throw new InvalidTokenException(token);
				}
			}

			lastToken = token;
		}

		while (!operatorStack.isEmpty()) {
			final IExecutable<E> top = operatorStack.pop();
			if (top instanceof InfixCompiler.BracketMarker) throw new IllegalArgumentException("Unmatched brackets");
			setArgCount(top, 0);
			output.add(top);
		}

		return new ExecutableList<E>(output);
	}

	protected void setArgCount(final IExecutable<E> symbol, final int argCount) {
		if (symbol instanceof SymbolReference<?>) ((SymbolReference<?>)symbol).setArgumentsCount(argCount).setReturnsCount(1);
	}

	private int popUntilBracket(List<IExecutable<E>> output, Stack<IExecutable<E>> operatorStack) {
		try {
			while (true) {
				final IExecutable<E> op = operatorStack.peek(0);
				if (op instanceof InfixCompiler.BracketMarker) return ((InfixCompiler<?>.BracketMarker)op).incrementArgCount();
				operatorStack.pop();
				output.add(op);
			}
		} catch (StackUnderflowException e) {
			throw new IllegalArgumentException("Unmatched brackets");
		}
	}
}
