package openmods.calc.parsing.ast;

import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.parsing.token.TokenUtils;

public class QuotedParser<N> implements IAstParser<N> {

	public interface IQuotedExprNodeFactory<E> {
		public E createValueNode(Token token);

		public E createBracketNode(String openingBracket, String closingBracket, List<E> children);
	}

	private final IQuotedExprNodeFactory<N> exprNodeFactory;

	public QuotedParser(IQuotedExprNodeFactory<N> exprNodeFactory) {
		this.exprNodeFactory = exprNodeFactory;
	}

	private N parseQuotedNode(PeekingIterator<Token> input) {
		final Token token = input.next();
		if (token.type == TokenType.LEFT_BRACKET) {
			return parseNestedQuotedNode(token.value, input);
		} else {
			return exprNodeFactory.createValueNode(token);
		}
	}

	private N parseNestedQuotedNode(String openingBracket, PeekingIterator<Token> input) {
		final List<N> children = Lists.newArrayList();
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
	public N parse(IParserState<N> state, PeekingIterator<Token> input) {
		return parseQuotedNode(input);
	}

}
