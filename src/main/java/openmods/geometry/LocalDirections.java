package openmods.geometry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.minecraft.util.EnumFacing;

public class LocalDirections {

	public final EnumFacing front;

	public final EnumFacing back;

	public final EnumFacing top;

	public final EnumFacing bottom;

	public final EnumFacing left;

	public final EnumFacing right;

	private LocalDirections(EnumFacing front, EnumFacing top) {
		this.front = front;
		this.back = front.getOpposite();
		this.top = top;
		this.bottom = top.getOpposite();

		final HalfAxis frontHa = HalfAxis.fromEnumFacing(front);
		final HalfAxis topHa = HalfAxis.fromEnumFacing(top);

		this.right = frontHa.cross(topHa).dir;
		this.left = topHa.cross(frontHa).dir;
	}

	private static final Table<EnumFacing, EnumFacing, LocalDirections> frontAndTop = HashBasedTable.create();

	static {
		for (EnumFacing front : EnumFacing.VALUES)
			for (EnumFacing top : EnumFacing.VALUES) {
				if (top.getAxis() != front.getAxis())
					frontAndTop.put(front, top, new LocalDirections(front, top));
			}
	}

	public static LocalDirections fromFrontAndTop(EnumFacing front, EnumFacing top) {
		return frontAndTop.get(front, top);
	}
}
