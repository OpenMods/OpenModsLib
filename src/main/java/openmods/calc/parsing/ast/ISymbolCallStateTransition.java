package openmods.calc.parsing.ast;

import java.util.List;

public interface ISymbolCallStateTransition<N> {
	public IParserState<N> getState();

	public N createRootNode(List<N> children);
}