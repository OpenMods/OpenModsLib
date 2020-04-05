package openmods.geometry;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.vecmath.Vector3f;
import net.minecraft.util.Direction;
import org.apache.commons.lang3.tuple.Pair;

public class FaceClassifier {

	private static final Map<Direction, Vector3f> BASES = Maps.newEnumMap(Direction.class);

	static {
		for (Direction e : Direction.values())
			BASES.put(e, new Vector3f(e.getXOffset(), e.getYOffset(), e.getZOffset()));
	}

	private final List<Pair<Vector3f, Direction>> sideOrder = Lists.newArrayList();

	public FaceClassifier(Collection<Direction> sideOrder) {
		this.sideOrder.addAll(Collections2.transform(sideOrder, input -> Pair.of(BASES.get(input), input)));
	}

	public Optional<Direction> classify(Vector3f normalVec) {
		for (Pair<Vector3f, Direction> e : sideOrder) {
			final Vector3f base = e.getKey();
			if (normalVec.equals(base)) return Optional.of(e.getValue());

			// cos > 0 only in 0..90 deg
			final double angleCos = base.dot(normalVec);
			if (angleCos > 0) return Optional.of(e.getValue());
		}

		return Optional.empty();
	}
}
