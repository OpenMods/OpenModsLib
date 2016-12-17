package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.SymbolMap;

public interface IBindPattern {
	public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value);
}