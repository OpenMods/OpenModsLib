package openmods.stencil;

public class StencilBitAllocation implements Comparable<StencilBitAllocation> {

	public final int bit;

	public final int mask;

	StencilBitAllocation(int bit) {
		this.bit = bit;
		this.mask = 1 << bit;
	}

	@Override
	public int compareTo(StencilBitAllocation o) {
		return bit - o.bit;
	}
}
