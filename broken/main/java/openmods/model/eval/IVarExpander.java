package openmods.model.eval;

import java.util.Map;

@FunctionalInterface
public interface IVarExpander {
	Map<String, Float> expand(Map<String, Float> args);
}
