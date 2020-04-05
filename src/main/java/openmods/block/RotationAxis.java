package openmods.block;

import net.minecraft.util.Direction;

class RotationAxis {
	public static final Direction[] NO_AXIS = {};
	public static final Direction[] SINGLE_AXIS = { Direction.UP, Direction.DOWN };
	public static final Direction[] THREE_AXIS = Direction.values();
}