package openmods.calc.parsing;

import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.List;

public class QuotedParser<E> implements IAstParser<E> {

	private final IValueParser<E> valueParser;

	private final IExprNodeFactory<E> exprNodeFactory;

	public QuotedParser(IValueParser<E> valueParser, IExprNodeFactory<E> exprNodeFactory) {
		this.valueParser = valueParser;
		this.exprNodeFactory = exprNodeFactory;
	}

	private IExprNode<E> parseQuotedNode(PeekingIterator<Token> input) {
		final Token token = input.next();
		if (token.type == TokenType.LEFT_BRACKET) {
			return parseNestedQuotedNode(token.value, input, exprNodeFactory);
		} else if (token.type.isValue()) {
			final E value = valueParser.parseToken(token);
			return exprNodeFactory.createValueNode(value);
		} else {
			return exprNodeFactory.createRawValueNode(token);
		}
	}

	private IExprNode<E> parseNestedQuotedNode(String openingBracket, PeekingIterator<Token> input, IExprNodeFactory<E> exprNodeFactory) {
		final List<IExprNode<E>> children = Lists.newArrayList();
		while (true) {
			final Token token = input.peek();
			if (token.type == TokenType.RIGHT_BRACKET) {
				TokenUtils.checkIsValidBracketPair(openingBracket, token.value);
				input.next();
				return exprNodeFactory.createBracketNode(openingBracket, token.value, children);
			} else {
				children.add(parseQuotedNode(input));
			}
		}
	}

	@Override
	public IExprNode<E> parse(PeekingIterator<Token> input) {
		return parseQuotedNode(input);
	}

}
