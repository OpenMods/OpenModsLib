package openmods.calc.types.multi;

import java.util.Collection;
import openmods.calc.Frame;
import openmods.calc.symbol.SymbolMap;

public interface IBindPattern {
	public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value);

	public void listBoundVars(Collection<String> output);

	public String serialize();
}