package openmods.geometry;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.vecmath.Vector3f;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

public class FaceClassifier {

	private static final Map<EnumFacing, Vector3f> BASES = Maps.newEnumMap(EnumFacing.class);

	static {
		for (EnumFacing e : EnumFacing.VALUES)
			BASES.put(e, new Vector3f(e.getFrontOffsetX(), e.getFrontOffsetY(), e.getFrontOffsetZ()));
	}

	private final List<Pair<Vector3f, EnumFacing>> sideOrder = Lists.newArrayList();

	public FaceClassifier(Collection<EnumFacing> sideOrder) {
		this.sideOrder.addAll(Collections2.transform(sideOrder, input -> Pair.of(BASES.get(input), input)));
	}

	public Optional<EnumFacing> classify(Vector3f normalVec) {
		for (Pair<Vector3f, EnumFacing> e : sideOrder) {
			final Vector3f base = e.getKey();
			if (normalVec.equals(base)) return Optional.of(e.getValue());

			// cos > 0 only in 0..90 deg
			final double angleCos = base.dot(normalVec);
			if (angleCos > 0) return Optional.of(e.getValue());
		}

		return Optional.empty();
	}
}
