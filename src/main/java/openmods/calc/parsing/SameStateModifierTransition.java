package openmods.calc.parsing;

import openmods.calc.parsing.ICompilerState.IModifierStateTransition;

public abstract class SameStateModifierTransition<E> implements IModifierStateTransition<E> {

	private final ICompilerState<E> parentState;

	public SameStateModifierTransition(ICompilerState<E> parentState) {
		this.parentState = parentState;
	}

	@Override
	public ICompilerState<E> getState() {
		return this.parentState;
	}

}
