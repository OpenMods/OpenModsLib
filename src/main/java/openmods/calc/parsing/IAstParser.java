package openmods.calc.parsing;

import com.google.common.collect.PeekingIterator;

public interface IAstParser<E> {
	public IExprNode<E> parse(PeekingIterator<Token> input);
}
