package openmods.calc.parsing;

import java.util.List;

import openmods.calc.*;
import openmods.utils.Stack;
import openmods.utils.Stack.StackUnderflowException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class InfixCompiler<E> implements ICompiler<E> {

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	public InfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	@Override
	public IExecutable<E> compile(Iterable<Token> input) {
		final Stack<IExprNode<E>> nodeStack = Stack.create();
		final Stack<Optional<Operator<E>>> operatorStack = Stack.create();

		final BinaryOperator<E> defaultOperator = operators.getDefaultOperator();

		Token lastToken = null;

		for (Token token : input) {
			if (defaultOperator != null &&
					token.type.canInsertDefaultOp() &&
					lastToken != null &&
					lastToken.type.isValue()) {
				pushOperator(nodeStack, operatorStack, defaultOperator);
				lastToken = new Token(TokenType.OPERATOR, defaultOperator.id);
			}

			if (token.type.isValue()) {
				final E value = valueParser.parseToken(token);
				nodeStack.push(new ValueNode<E>(value));
			} else if (token.type.isSymbol()) {
				Preconditions.checkArgument(token.type != TokenType.SYMBOL_WITH_ARGS, "Symbol '%s' can't be used in infix mode", token.value);
				nodeStack.push(createSymbolNode(token));
			} else {
				switch (token.type) {
					case LEFT_BRACKET:
						operatorStack.push(Optional.<Operator<E>> absent());
						if (lastToken == null || !lastToken.type.isSymbol()) nodeStack.push(createBracketNode());
						break;
					case RIGHT_BRACKET: {
						Preconditions.checkNotNull(lastToken, "Right bracket on invalid postion");
						popUntilBracket(nodeStack, operatorStack);
						if (lastToken.type != TokenType.LEFT_BRACKET) appendNodeChild(nodeStack);
						break;
					}
					case SEPARATOR: {
						Preconditions.checkNotNull(lastToken, "Comma on invalid postion");
						popUntilBracket(nodeStack, operatorStack);
						appendNodeChild(nodeStack);
						operatorStack.push(Optional.<Operator<E>> absent());
						break;
					}
					case OPERATOR: {
						final Operator<E> op;
						if (lastToken == null || lastToken.type.isNextOpUnary()) {
							op = operators.getUnaryOperator(token.value);
							Preconditions.checkArgument(op != null, "No unary version of operator: %s", token.value);
						} else {
							op = operators.getBinaryOperator(token.value);
							Preconditions.checkArgument(op != null, "Invalid operator: %s", token.value);
						}

						pushOperator(nodeStack, operatorStack, op);
						break;
					}
					default:
						throw new InvalidTokenException(token);
				}
			}

			lastToken = token;
		}

		while (!operatorStack.isEmpty()) {
			final Optional<Operator<E>> e = operatorStack.pop();
			if (e.isPresent()) pushOperator(nodeStack, e.get());
			else throw new IllegalArgumentException("Unmatched brackets");
		}

		Preconditions.checkState(nodeStack.size() == 1, "Not valid infix expression");
		final List<IExecutable<E>> output = Lists.newArrayList();
		nodeStack.pop().flatten(output);
		return new ExecutableList<E>(output);
	}

	protected SymbolNode<E> createSymbolNode(Token token) {
		return new SymbolNode<E>(token.value);
	}

	protected IInnerNode<E> createBracketNode() {
		return new NullNode<E>();
	}

	private void pushOperator(Stack<IExprNode<E>> output, Stack<Optional<Operator<E>>> operatorStack, Operator<E> newOp) {
		while (!operatorStack.isEmpty()) {
			final Optional<Operator<E>> top = operatorStack.peek(0);
			if (!top.isPresent()) break;

			final Operator<E> topOp = top.get();
			if (!newOp.isLessThan(topOp)) break;

			operatorStack.pop();
			pushOperator(output, topOp);
		}

		operatorStack.push(Optional.of(newOp));
	}

	private void appendNodeChild(Stack<IExprNode<E>> output) {
		if (output.size() > 1) {
			final IExprNode<E> t = output.peek(1);
			if (t instanceof IInnerNode) {
				final IExprNode<E> top = output.pop();
				((IInnerNode<E>)t).addChild(top);
			}
		}
	}

	private void popUntilBracket(Stack<IExprNode<E>> output, Stack<Optional<Operator<E>>> operatorStack) {
		try {
			while (true) {
				final Optional<Operator<E>> e = operatorStack.pop();
				if (e.isPresent()) pushOperator(output, e.get());
				else break;
			}
		} catch (StackUnderflowException e) {
			throw new IllegalArgumentException("Unmatched brackets");
		}
	}

	private void pushOperator(Stack<IExprNode<E>> output, Operator<E> op) {
		if (op instanceof BinaryOperator) {
			final IExprNode<E> right = output.pop();
			final IExprNode<E> left = output.pop();
			output.push(operators.getExprNodeForOperator((BinaryOperator<E>)op, left, right));
		} else if (op instanceof UnaryOperator) {
			final IExprNode<E> arg = output.pop();
			output.push(operators.getExprNodeForOperator((UnaryOperator<E>)op, arg));
		} else throw new IllegalStateException("Unknown type of operator: " + op.getClass());
	}
}
