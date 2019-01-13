package openmods.model.eval;

import java.util.Map;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IJoint;

@FunctionalInterface
public interface ITransformEvaluator {

	TRSRTransformation evaluate(IJoint joint, Map<String, Float> args);

}
