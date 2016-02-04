package openmods.utils.bitstream;

import java.io.IOException;

public abstract class InputBitStreamBase {

	private final int initialMask;

	private int mask;

	private int currentByte;

	public InputBitStreamBase(int initialMask) {
		this.initialMask = initialMask;
	}

	protected abstract int nextByte() throws IOException;

	public boolean readBit() throws IOException {
		if (mask == 0) {
			currentByte = nextByte();
			mask = initialMask;
		}

		boolean bit = (currentByte & mask) != 0;
		mask >>= 1;

		return bit;
	}

	public abstract int bytesRead();
}
