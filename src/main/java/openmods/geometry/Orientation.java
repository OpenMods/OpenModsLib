package openmods.geometry;

import net.minecraftforge.common.util.ForgeDirection;

public enum Orientation {
	// name: first letter of Y direction + first letter of Z direction
	TN(ForgeDirection.UP, ForgeDirection.NORTH),
	TS(ForgeDirection.UP, ForgeDirection.SOUTH), // orientation aligned with world axes
	TE(ForgeDirection.UP, ForgeDirection.EAST),
	TW(ForgeDirection.UP, ForgeDirection.WEST),

	BN(ForgeDirection.DOWN, ForgeDirection.NORTH),
	BS(ForgeDirection.DOWN, ForgeDirection.SOUTH),
	BE(ForgeDirection.DOWN, ForgeDirection.EAST),
	BW(ForgeDirection.DOWN, ForgeDirection.WEST),

	NT(ForgeDirection.NORTH, ForgeDirection.UP),
	NE(ForgeDirection.NORTH, ForgeDirection.EAST),
	NW(ForgeDirection.NORTH, ForgeDirection.WEST),
	NB(ForgeDirection.NORTH, ForgeDirection.DOWN),

	ST(ForgeDirection.SOUTH, ForgeDirection.UP),
	SE(ForgeDirection.SOUTH, ForgeDirection.EAST),
	SW(ForgeDirection.SOUTH, ForgeDirection.WEST),
	SB(ForgeDirection.SOUTH, ForgeDirection.DOWN),

	WT(ForgeDirection.WEST, ForgeDirection.UP),
	WN(ForgeDirection.WEST, ForgeDirection.NORTH),
	WS(ForgeDirection.WEST, ForgeDirection.SOUTH),
	WB(ForgeDirection.WEST, ForgeDirection.DOWN),

	ET(ForgeDirection.EAST, ForgeDirection.UP),
	EN(ForgeDirection.EAST, ForgeDirection.NORTH),
	ES(ForgeDirection.EAST, ForgeDirection.SOUTH),
	EB(ForgeDirection.EAST, ForgeDirection.DOWN);

	public final ForgeDirection x; // +X, east

	public final ForgeDirection y; // +Y, top

	public final ForgeDirection z; // +Z, south

	private Orientation(ForgeDirection y, ForgeDirection z) {
		this.z = z;
		this.y = y;
		this.x = y.getRotation(z);
	}
}