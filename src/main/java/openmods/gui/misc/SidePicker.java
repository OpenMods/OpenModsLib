package openmods.gui.misc;

import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class SidePicker {
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
		public final Vector3f coord;

		public HitCoord(Side side, Vector3f coord) {
			this.side = side;
			this.coord = coord;
		}
	}

	private final float negX;
	private final float negY;
	private final float negZ;
	private final float posX;
	private final float posY;
	private final float posZ;

	public SidePicker(float negX, float negY, float negZ, float posX, float posY, float posZ) {
		this.negX = negX;
		this.negY = negY;
		this.negZ = negZ;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
	}

	public SidePicker(float halfSize) {
		negX = negY = negZ = -halfSize;
		posX = posY = posZ = +halfSize;
	}

	private Vector3f calculateXPoint(Vector3f near, Vector3f diff, float x) {
		float p = (x - near.getX()) / diff.getX();

		float y = near.getY() + diff.getY() * p;
		float z = near.getZ() + diff.getZ() * p;

		if (negY <= y && y <= posY && negZ <= z && z <= posZ) {
			return new Vector3f(x, y, z);
		}

		return null;
	}

	private Vector3f calculateYPoint(Vector3f near, Vector3f diff, float y) {
		float p = (y - near.getY()) / diff.getY();

		float x = near.getX() + diff.getX() * p;
		float z = near.getZ() + diff.getZ() * p;

		if (negX <= x && x <= posX && negZ <= z && z <= posZ) {
			return new Vector3f(x, y, z);
		}

		return null;
	}

	private Vector3f calculateZPoint(Vector3f near, Vector3f diff, float z) {
		float p = (z - near.getZ()) / diff.getZ();

		float x = near.getX() + diff.getX() * p;
		float y = near.getY() + diff.getY() * p;

		if (negX <= x && x <= posX && negY <= y && y <= posY) {
			return new Vector3f(x, y, z);
		}

		return null;
	}

	private static void addPoint(BiConsumer<Side, Vector3f> map, Side side, Vector3f value) {
		if (value != null) {
			map.accept(side, value);
		}
	}

	public void calculateHitPoints(Vector3f near, Vector3f far, BiConsumer<Side, Vector3f> output) {
		Vector3f diff = far.copy();
		diff.sub(near);

		addPoint(output, Side.XNeg, calculateXPoint(near, diff, negX));
		addPoint(output, Side.XPos, calculateXPoint(near, diff, posX));

		addPoint(output, Side.YNeg, calculateYPoint(near, diff, negY));
		addPoint(output, Side.YPos, calculateYPoint(near, diff, posY));

		addPoint(output, Side.ZNeg, calculateZPoint(near, diff, negZ));
		addPoint(output, Side.ZPos, calculateZPoint(near, diff, posZ));
	}

	private static class ClosestPointFinder implements BiConsumer<Side, Vector3f> {
		private final Vector3f near;

		Side minSide;
		Vector3f minHit;
		double minDist = Double.MAX_VALUE;

		private ClosestPointFinder(Vector3f near) {
			this.near = near;
		}

		@Override
		public void accept(Side side, Vector3f hit) {
			final Vector3f dist = hit.copy();
			dist.sub(near);
			double distSqr = dist.dot(dist);
			if (distSqr < minDist) {
				minDist = distSqr;
				minSide = side;
				minHit = hit;
			}
		}

		@Nullable
		public HitCoord getClosest() {
			if (minSide != null && minHit != null) {
				return new HitCoord(minSide, minHit);
			}
			return null;
		}
	}

	public HitCoord getNearestHit(Vector3f near, Vector3f far) {
		ClosestPointFinder finder = new ClosestPointFinder(near);
		calculateHitPoints(near, far, finder);
		return finder.getClosest();
	}

}