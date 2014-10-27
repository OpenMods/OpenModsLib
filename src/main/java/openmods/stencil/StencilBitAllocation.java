package openmods.stencil;

public class StencilBitAllocation {

	public final int bit;

	public final int mask;

	StencilBitAllocation(int bit) {
		this.bit = bit;
		this.mask = 1 << bit;
	}
}
