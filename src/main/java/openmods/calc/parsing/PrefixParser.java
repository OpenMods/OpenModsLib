package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import openmods.calc.BinaryOperator;
import openmods.calc.BinaryOperator.Associativity;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryOperator;

public class PrefixParser<E> implements IAstParser<E> {

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	private final IExprNodeFactory<E> exprNodeFactory;

	public PrefixParser(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		this.valueParser = valueParser;
		this.operators = operators;
		this.exprNodeFactory = exprNodeFactory;
	}

	private static Token next(Iterator<Token> input) {
		try {
			return input.next();
		} catch (NoSuchElementException e) {
			throw new UnfinishedExpressionException();
		}
	}

	protected IExprNode<E> parseNode(PeekingIterator<Token> input) {
		final Token token = next(input);
		if (token.type.isValue()) {
			final E value = valueParser.parseToken(token);
			return exprNodeFactory.createValueNode(value);
		}

		if (token.type == TokenType.SYMBOL) {
			final ImmutableList<IExprNode<E>> emptyArgs = ImmutableList.of();
			return exprNodeFactory.createSymbolExprNodeFactory(token.value).createRootSymbolNode(emptyArgs);
		}

		if (token.type == TokenType.MODIFIER) return parseModifierNode(token.value, input);

		if (token.type == TokenType.LEFT_BRACKET)
			return parseNestedNode(token.value, input);

		throw new IllegalArgumentException("Unexpected token: " + token);
	}

	private IExprNode<E> parseNestedNode(String openingBracket, PeekingIterator<Token> input) {
		final String closingBracket = TokenUtils.getClosingBracket(openingBracket);

		if (openingBracket.equals("(")) {
			final Token operationToken = next(input);

			final String operationName = operationToken.value;
			if (operationToken.type == TokenType.SYMBOL) {
				final ISymbolExprNodeFactory<E> symbolNodeFactory = exprNodeFactory.createSymbolExprNodeFactory(operationName);
				final List<IExprNode<E>> args = collectArgs(openingBracket, closingBracket, input, symbolNodeFactory);
				return symbolNodeFactory.createRootSymbolNode(args);
				// no modifiers allowed on this position (yet), so assuming operator
			} else if (operationToken.type == TokenType.OPERATOR || operationToken.type == TokenType.MODIFIER) {
				final List<IExprNode<E>> args = collectArgs(openingBracket, closingBracket, input, exprNodeFactory);
				if (args.size() == 1) {
					final UnaryOperator<E> unaryOperator = operators.getUnaryOperator(operationName);
					Preconditions.checkState(unaryOperator != null, "Invalid unary operator '%s'", operationName);
					return exprNodeFactory.createUnaryOpNode(unaryOperator, args.get(0));
				} else if (args.size() > 1) {
					final BinaryOperator<E> binaryOperator = operators.getBinaryOperator(operationName);
					Preconditions.checkState(binaryOperator != null, "Invalid binary operator '%s'", operationName);
					return compileBinaryOpNode(binaryOperator, args);
				} else {
					throw new IllegalArgumentException("Called operator " + operationName + " without any arguments");
				}
			} else {
				// TODO: consider doing nested statements on first entry?
				throw new IllegalArgumentException("Unexpected token: " + operationToken);
			}
		} else {
			// not parenthesis, so probably data structure
			final List<IExprNode<E>> args = collectArgs(openingBracket, closingBracket, input, exprNodeFactory);
			return exprNodeFactory.createBracketNode(openingBracket, closingBracket, args);
		}
	}

	private List<IExprNode<E>> collectArgs(String openingBracket, String closingBracket, PeekingIterator<Token> input, IAstParserProvider<E> argParserProvider) {
		final List<IExprNode<E>> args = Lists.newArrayList();
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
				final IAstParser<E> newParser = argParserProvider.getParser();
				final IExprNode<E> parsedNode = newParser.parse(input);
				args.add(parsedNode);
			}
		}
		return args;
	}

	private IExprNode<E> parseModifierNode(String modifier, PeekingIterator<Token> input) {
		final IModifierExprNodeFactory<E> newNodeFactory = exprNodeFactory.createModifierExprNodeFactory(modifier);
		final IAstParser<E> newParser = newNodeFactory.getParser();
		final IExprNode<E> parsedNode = newParser.parse(input);
		return newNodeFactory.createRootModifierNode(parsedNode);
	}

	private IExprNode<E> compileBinaryOpNode(BinaryOperator<E> op, List<IExprNode<E>> args) {
		if (op.associativity == Associativity.LEFT) {
			IExprNode<E> left = args.get(0);
			IExprNode<E> right = args.get(1);

			for (int i = 2; i < args.size(); i++) {
				left = exprNodeFactory.createBinaryOpNode(op, left, right);
				right = args.get(i);
			}

			return exprNodeFactory.createBinaryOpNode(op, left, right);
		} else {
			final int lastArg = args.size() - 1;
			IExprNode<E> left = args.get(lastArg - 1);
			IExprNode<E> right = args.get(lastArg);

			for (int i = lastArg - 2; i >= 0; i--) {
				right = exprNodeFactory.createBinaryOpNode(op, left, right);
				left = args.get(i);
			}

			return exprNodeFactory.createBinaryOpNode(op, left, right);
		}

	}

	@Override
	public IExprNode<E> parse(PeekingIterator<Token> input) {
		return parseNode(input);
	}

}
