package openmods.utils.bitstream;

import java.io.IOException;

import openmods.utils.io.IByteSource;

public class InputBitStream extends InputBitStreamBase {

	private int byteCounter;

	private final IByteSource source;

	public InputBitStream(IByteSource source) {
		super(0x80);
		this.source = source;
	}

	@Override
	protected int nextByte() throws IOException {
		final int nextByte = source.nextByte();
		byteCounter++;
		return nextByte;
	}

	@Override
	public int bytesRead() {
		return byteCounter;
	}

}
