package openmods.calc.parsing;

import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.List;

public class QuotedParser<E> implements IAstParser<E> {

	public interface IQuotedExprNodeFactory<E> {
		public IExprNode<E> createValueNode(E value);

		public IExprNode<E> createValueNode(Token token);

		public IExprNode<E> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<E>> children);

	}

	private final IQuotedExprNodeFactory<E> exprNodeFactory;

	public QuotedParser(IValueParser<E> valueParser, IQuotedExprNodeFactory<E> exprNodeFactory) {
		this.exprNodeFactory = exprNodeFactory;
	}

	private IExprNode<E> parseQuotedNode(PeekingIterator<Token> input) {
		final Token token = input.next();
		if (token.type == TokenType.LEFT_BRACKET) {
			return parseNestedQuotedNode(token.value, input, exprNodeFactory);
		} else {
			return exprNodeFactory.createValueNode(token);
		}
	}

	private IExprNode<E> parseNestedQuotedNode(String openingBracket, PeekingIterator<Token> input, IQuotedExprNodeFactory<E> exprNodeFactory) {
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
	public IExprNode<E> parse(ICompilerState<E> state, PeekingIterator<Token> input) {
		return parseQuotedNode(input);
	}

}
