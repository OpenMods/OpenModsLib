package openmods.gui.misc;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import openmods.utils.render.ProjectionHelper;

public class SidePicker {

	private static final ProjectionHelper projectionHelper = new ProjectionHelper();

	public enum Side {
		XNeg,
		XPos,
		YNeg,
		YPos,
		ZNeg,
		ZPos;

		public static Side fromForgeDirection(Direction dir) {
			switch (dir) {
				case WEST:
					return XNeg;
				case EAST:
					return XPos;
				case DOWN:
					return YNeg;
				case UP:
					return YPos;
				case NORTH:
					return ZNeg;
				case SOUTH:
					return ZPos;
				default:
					break;
			}
			return null;
		}

		public Direction toForgeDirection() {
			switch (this) {
				case XNeg:
					return Direction.WEST;
				case XPos:
					return Direction.EAST;
				case YNeg:
					return Direction.DOWN;
				case YPos:
					return Direction.UP;
				case ZNeg:
					return Direction.NORTH;
				case ZPos:
					return Direction.SOUTH;
				default:
					throw new IllegalArgumentException(toString());
			}
		}
	}

	public static class HitCoord {
		public final Side side;
		public final Vector3d coord;

		public HitCoord(Side side, Vector3d coord) {
			this.side = side;
			this.coord = coord;
		}
	}

	private final double negX, negY, negZ;
	private final double posX, posY, posZ;

	public SidePicker(double negX, double negY, double negZ, double posX, double posY, double posZ) {
		this.negX = negX;
		this.negY = negY;
		this.negZ = negZ;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
	}

	public SidePicker(double halfSize) {
		negX = negY = negZ = -halfSize;
		posX = posY = posZ = +halfSize;
	}

	private static Vector3d getMouseVector(float z) {
		// TODO 1.14 supply from caller (was: mouseX, mouseY)
		return projectionHelper.unproject(0, 0, z);
	}

	private Vector3d calculateXPoint(Vector3d near, Vector3d diff, double x) {
		double p = (x - near.x) / diff.x;

		double y = near.y + diff.y * p;
		double z = near.z + diff.z * p;

		if (negY <= y && y <= posY && negZ <= z && z <= posZ) return new Vector3d(x, y, z);

		return null;
	}

	private Vector3d calculateYPoint(Vector3d near, Vector3d diff, double y) {
		double p = (y - near.y) / diff.y;

		double x = near.x + diff.x * p;
		double z = near.z + diff.z * p;

		if (negX <= x && x <= posX && negZ <= z && z <= posZ) return new Vector3d(x, y, z);

		return null;
	}

	private Vector3d calculateZPoint(Vector3d near, Vector3d diff, double z) {
		double p = (z - near.z) / diff.z;

		double x = near.x + diff.x * p;
		double y = near.y + diff.y * p;

		if (negX <= x && x <= posX && negY <= y && y <= posY) return new Vector3d(x, y, z);

		return null;
	}

	private static void addPoint(Map<Side, Vector3d> map, Side side, Vector3d value) {
		if (value != null) map.put(side, value);
	}

	private Map<Side, Vector3d> calculateHitPoints(Vector3d near, Vector3d far) {
		Vector3d diff = far.subtract(near);

		Map<Side, Vector3d> result = Maps.newEnumMap(Side.class);
		addPoint(result, Side.XNeg, calculateXPoint(near, diff, negX));
		addPoint(result, Side.XPos, calculateXPoint(near, diff, posX));

		addPoint(result, Side.YNeg, calculateYPoint(near, diff, negY));
		addPoint(result, Side.YPos, calculateYPoint(near, diff, posY));

		addPoint(result, Side.ZNeg, calculateZPoint(near, diff, negZ));
		addPoint(result, Side.ZPos, calculateZPoint(near, diff, posZ));
		return result;
	}

	public Map<Side, Vector3d> calculateMouseHits() {
		projectionHelper.updateMatrices();
		Vector3d near = getMouseVector(0);
		Vector3d far = getMouseVector(1);

		return calculateHitPoints(near, far);
	}

	public HitCoord getNearestHit() {
		projectionHelper.updateMatrices();
		Vector3d near = getMouseVector(0);
		Vector3d far = getMouseVector(1);

		Map<Side, Vector3d> hits = calculateHitPoints(near, far);

		if (hits.isEmpty()) return null;

		Side minSide = null;
		double minDist = Double.MAX_VALUE;

		// yeah, I know there are two entries max, but... meh
		for (Map.Entry<Side, Vector3d> e : hits.entrySet()) {
			double dist = e.getValue().subtract(near).length();
			if (dist < minDist) {
				minDist = dist;
				minSide = e.getKey();
			}
		}

		if (minSide == null) return null; // !?

		return new HitCoord(minSide, hits.get(minSide));
	}

}