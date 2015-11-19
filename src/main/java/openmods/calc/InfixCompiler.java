package openmods.calc;

import java.util.List;

import openmods.utils.Stack;
import openmods.utils.Stack.StackUnderflowException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class InfixCompiler<E> implements ICompiler<E> {

	private final IExecutable<E> BRACKET_MARKER = new IExecutable<E>() {
		@Override
		public void execute(CalculatorContext<E> context) {
			throw new UnsupportedOperationException();
		}
	};

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
			if (token.type.isValue) {
				final E value = valueParser.parseToken(token);
				output.add(Constant.create(value));
			} else {
				switch (token.type) {
					case LEFT_BRACKET:
						operatorStack.push(BRACKET_MARKER);
						break;
					case RIGHT_BRACKET: {
						popUntilBracket(output, operatorStack);
						operatorStack.pop(); // left bracket
						if (!operatorStack.isEmpty()) {
							final IExecutable<E> top = operatorStack.peek(0);
							if (top instanceof ISymbol) {
								operatorStack.pop();
								output.add(top);
							}
						}
						break;
					}
					case SEPARATOR: {
						popUntilBracket(output, operatorStack);
						break;
					}
					case OPERATOR: {
						final IOperator<E> op;
						if (lastToken == null || lastToken.type.nextOpInfix) {
							op = operators.getUnaryVariant(token.value);
							Preconditions.checkArgument(op != null, "No unary version of operator: %s", token.value);
						} else
						{
							op = operators.get(token.value);
							Preconditions.checkArgument(op != null, "Invalid operator: %s", token.value);
						}

						while (!operatorStack.isEmpty()) {
							final IExecutable<E> top = operatorStack.peek(0);
							if (!(top instanceof IOperator)) break;

							final IOperator<E> topOp = (IOperator<E>)top;
							if (!topOp.getAssociativity().compare(op, topOp)) break;

							operatorStack.pop();
							output.add(top);
						}

						operatorStack.push(op);
						break;
					}
					case SYMBOL:
						operatorStack.push(new DelayedSymbol<E>(token.value));
						break;
					case CONSTANT:
					case IMMEDIATE_SYMBOL:
						output.add(new DelayedSymbol<E>(token.value));
						break;
					default:
						throw new InvalidTokenException(token);
				}
			}

			lastToken = token;
		}

		while (!operatorStack.isEmpty()) {
			final IExecutable<E> top = operatorStack.pop();
			if (top == BRACKET_MARKER) throw new IllegalArgumentException("Unmatched brackets");
			output.add(top);
		}

		return new ExecutableList<E>(output);
	}

	private void popUntilBracket(List<IExecutable<E>> output, Stack<IExecutable<E>> operatorStack) {
		try {
			while (true) {
				final IExecutable<E> op = operatorStack.peek(0);
				if (op == BRACKET_MARKER) break;
				operatorStack.pop();
				output.add(op);
			}
		} catch (StackUnderflowException e) {
			throw new IllegalArgumentException("Unmatched brackets");
		}
	}
}
