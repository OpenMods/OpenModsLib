package openmods.utils.bitstream;

import java.io.IOException;

import openmods.utils.io.IByteSink;

public class OutputBitStream {

	private int buffer;

	private int bitCount;

	private final IByteSink sink;

	private int byteCount;

	private final int flushBitCount;

	OutputBitStream(IByteSink sink, int flushBitCount) {
		this.flushBitCount = flushBitCount;
		this.sink = sink;
	}

	public OutputBitStream(IByteSink sink) {
		this(sink, 8);
	}

	private int padBuffer() {
		return buffer << (flushBitCount - bitCount);
	}

	public void writeBit(boolean bit) throws IOException {
		if (bitCount >= flushBitCount) flushBuffer(padBuffer(), false);
		else buffer <<= 1;
		if (bit) buffer |= 1;
		bitCount += 1;
	}

	public void flush() throws IOException {
		if (bitCount > 0) flushBuffer(padBuffer(), true);
	}

	protected void acceptByte(int b) throws IOException {
		sink.acceptByte(b);
		byteCount++;
	}

	protected void flushBuffer(int value, boolean isLastBit) throws IOException {
		flushBuffer(value);
	}

	protected final void flushBuffer(int value) throws IOException {
		acceptByte(value);
		bitCount = 0;
		buffer = 0;
	}

	public int bytesWritten() {
		return byteCount;
	}
}
