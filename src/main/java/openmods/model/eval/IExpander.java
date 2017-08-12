package openmods.model.eval;

import java.util.Map;

public interface IExpander {
	public Map<String, Float> expand(Map<String, Float> args);
}
