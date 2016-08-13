package openmods.calc.parsing;

import openmods.calc.parsing.ICompilerState.ISymbolCallStateTransition;

public abstract class SameStateSymbolTransition<E> implements ISymbolCallStateTransition<E> {

	private final ICompilerState<E> parentState;

	public SameStateSymbolTransition(ICompilerState<E> parentState) {
		this.parentState = parentState;
	}

	@Override
	public ICompilerState<E> getState() {
		return this.parentState;
	}

}
