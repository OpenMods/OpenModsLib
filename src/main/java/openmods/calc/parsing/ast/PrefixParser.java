package openmods.calc.parsing.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.parsing.token.TokenUtils;

public class PrefixParser<N, O extends IOperator<O>> implements IAstParser<N> {

	private final IOperatorDictionary<O> operators;

	private final INodeFactory<N, O> exprNodeFactory;

	public PrefixParser(IOperatorDictionary<O> operators, INodeFactory<N, O> nodeFactory) {
		this.operators = operators;
		this.exprNodeFactory = nodeFactory;
	}

	private static Token next(Iterator<Token> input) {
		try {
			return input.next();
		} catch (NoSuchElementException e) {
			throw new UnfinishedExpressionException();
		}
	}

	protected N parseNode(IParserState<N> state, PeekingIterator<Token> input) {
		final Token token = next(input);
		return parseNode(state, input, token);
	}

	private N parseNode(IParserState<N> state, PeekingIterator<Token> input, final Token firstToken) {
		if (firstToken.type.isValue()) return exprNodeFactory.createValueNode(firstToken);

		switch (firstToken.type) {
			case SYMBOL:
				return exprNodeFactory.createSymbolGetNode(firstToken.value);
			case MODIFIER:
				return parseModifierNode(firstToken.value, state, input);
			case LEFT_BRACKET:
				return parseNestedNode(firstToken.value, state, input);
			default:
				throw new IllegalArgumentException("Unexpected token: " + firstToken);
		}
	}

	private static <N> List<N> arg(N child) {
		return ImmutableList.of(child);
	}

	private static <N> List<N> args(N left, N right) {
		return ImmutableList.of(left, right);
	}

	private N parseNestedNode(String openingBracket, IParserState<N> state, PeekingIterator<Token> input) {
		final String closingBracket = TokenUtils.getClosingBracket(openingBracket);

		if (openingBracket.equals("(")) {
			final Token operationToken = next(input);

			final String operationName = operationToken.value;
			if (operationToken.type == TokenType.SYMBOL) {
				final ISymbolCallStateTransition<N> stateTransition = state.getStateForSymbolCall(operationName);
				final List<N> args = collectArgs(openingBracket, closingBracket, input, stateTransition.getState());
				return stateTransition.createRootNode(args);
			} else if (operationToken.type == TokenType.OPERATOR) {
				final List<N> args = collectArgs(openingBracket, closingBracket, input, state);
				if (args.size() == 1) {
					final O unaryOperator = operators.getOperator(operationName, OperatorArity.UNARY);
					Preconditions.checkState(unaryOperator != null, "Invalid unary operator '%s'", operationName);
					return exprNodeFactory.createOpNode(unaryOperator, arg(args.get(0)));
				} else if (args.size() > 1) {
					final O binaryOperator = operators.getOperator(operationName, OperatorArity.BINARY);
					Preconditions.checkState(binaryOperator != null, "Invalid binary operator '%s'", operationName);
					return compileBinaryOpNode(binaryOperator, args);
				} else {
					throw new IllegalArgumentException("Called operator " + operationName + " without any arguments");
				}
			} else {
				// bit non-standard, but meh
				final N target = parseNode(state, input, operationToken);
				final List<N> args = collectArgs(openingBracket, closingBracket, input, state);
				return exprNodeFactory.createOpNode(operators.getDefaultOperator(), args(target, exprNodeFactory.createBracketNode(openingBracket, closingBracket, args)));
			}
		} else {
			// not parenthesis, so probably data structure
			final List<N> args = collectArgs(openingBracket, closingBracket, input, state);
			return exprNodeFactory.createBracketNode(openingBracket, closingBracket, args);
		}
	}

	private List<N> collectArgs(String openingBracket, String closingBracket, PeekingIterator<Token> input, IParserState<N> state) {
		final List<N> args = Lists.newArrayList();
		while (true) {
			final Token argToken = input.peek();
			if (argToken.type == TokenType.SEPARATOR) {
				// comma is whitespace
				next(input);
			} else if (argToken.type == TokenType.RIGHT_BRACKET) {
				Preconditions.checkState(argToken.value.equals(closingBracket), "Unmatched brackets: '%s' and '%s'", openingBracket, argToken.value);
				next(input);
				break;
			} else {
				final IAstParser<N> newParser = state.getParser();
				final N parsedNode = newParser.parse(state, input);
				args.add(parsedNode);
			}
		}
		return args;
	}

	private N parseModifierNode(String modifier, IParserState<N> state, PeekingIterator<Token> input) {
		final IModifierStateTransition<N> stateTransition = state.getStateForModifier(modifier);
		final IParserState<N> newState = stateTransition.getState();
		final IAstParser<N> newParser = newState.getParser();
		final N parsedNode = newParser.parse(newState, input);
		return stateTransition.createRootNode(parsedNode);
	}

	private N compileBinaryOpNode(O op, List<N> args) {
		final boolean isLeftAssociative = op.isLowerPriority(op);
		if (isLeftAssociative) {
			N left = args.get(0);
			N right = args.get(1);

			for (int i = 2; i < args.size(); i++) {
				left = exprNodeFactory.createOpNode(op, args(left, right));
				right = args.get(i);
			}

			return exprNodeFactory.createOpNode(op, args(left, right));
		} else {
			final int lastArg = args.size() - 1;
			N left = args.get(lastArg - 1);
			N right = args.get(lastArg);

			for (int i = lastArg - 2; i >= 0; i--) {
				right = exprNodeFactory.createOpNode(op, args(left, right));
				left = args.get(i);
			}

			return exprNodeFactory.createOpNode(op, args(left, right));
		}

	}

	@Override
	public N parse(IParserState<N> state, PeekingIterator<Token> input) {
		return parseNode(state, input);
	}

}
