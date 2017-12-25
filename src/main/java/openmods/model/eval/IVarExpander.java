package openmods.model.eval;

import java.util.Map;

@FunctionalInterface
public interface IVarExpander {
	public Map<String, Float> expand(Map<String, Float> args);
}
