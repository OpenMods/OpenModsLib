package openmods.model.eval;

import java.util.Map;
import net.minecraftforge.common.model.IModelState;

public interface IEvaluator {

	public IModelState evaluate(Map<String, Float> args);

}
