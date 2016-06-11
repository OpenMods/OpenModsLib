package openmods.geometry;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class BoundingBoxMap<T> {

	private final List<Map.Entry<AxisAlignedBB, T>> entries = Lists.newArrayList();

	public void addBox(AxisAlignedBB aabb, T value) {
		Preconditions.checkNotNull(aabb);
		entries.add(Maps.immutableEntry(aabb, value));
	}

	public Map.Entry<AxisAlignedBB, T> findEntryContainingPoint(Vec3 point) {
		for (Map.Entry<AxisAlignedBB, T> e : entries)
			if (e.getKey().isVecInside(point)) return e;

		return null;
	}

	public void findAllEntriesContainingPoint(Vec3 point, Collection<Map.Entry<AxisAlignedBB, T>> output) {
		for (Map.Entry<AxisAlignedBB, T> e : entries)
			if (e.getKey().isVecInside(point)) output.add(e);
	}

	public static <T> BoundingBoxMap<T> create() {
		return new BoundingBoxMap<T>();
	}
}
