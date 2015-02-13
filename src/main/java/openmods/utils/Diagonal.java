package openmods.utils;

import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.Maps;

public enum Diagonal {
	NW(ForgeDirection.NORTH, ForgeDirection.WEST),
	NE(ForgeDirection.NORTH, ForgeDirection.EAST),
	SW(ForgeDirection.SOUTH, ForgeDirection.WEST),
	SE(ForgeDirection.SOUTH, ForgeDirection.EAST);

	public final int offsetX;
	public final int offsetY;
	public final int offsetZ;

	private Diagonal(ForgeDirection a, ForgeDirection b) {
		this.offsetX = a.offsetX + b.offsetX;
		this.offsetY = a.offsetY + b.offsetY;
		this.offsetZ = a.offsetZ + b.offsetZ;
	}

	private static final Map<Diagonal, Diagonal> OPPOSITES = Maps.newEnumMap(Diagonal.class);

	private static final Map<Diagonal, Diagonal> CW = Maps.newEnumMap(Diagonal.class);

	private static final Map<Diagonal, Diagonal> CCW = Maps.newEnumMap(Diagonal.class);

	static {
		OPPOSITES.put(NW, SE);
		OPPOSITES.put(SE, NW);
		OPPOSITES.put(NE, SW);
		OPPOSITES.put(SW, NE);

		CW.put(NW, SW);
		CW.put(SW, SE);
		CW.put(SE, NE);
		CW.put(NE, NW);

		CCW.put(NW, NE);
		CCW.put(NE, SE);
		CCW.put(SE, SW);
		CCW.put(SW, NW);
	}

	public Diagonal getOpposite() {
		return OPPOSITES.get(this);
	}

	public Diagonal rotateCW() {
		return CW.get(this);
	}

	public Diagonal rotateCCW() {
		return CCW.get(this);
	}

	public static final Diagonal[] VALUES = values();
}
