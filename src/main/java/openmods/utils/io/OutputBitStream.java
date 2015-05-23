package openmods.utils.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public abstract class OutputBitStream {

	private int buffer;

	private int bitCount;

	private int byteCount;

	protected abstract void writeByte(int b) throws IOException;

	public void writeBit(boolean bit) throws IOException {
		if (bitCount >= 8) flushBuffer();
		buffer <<= 1;
		if (bit) buffer |= 1;
		bitCount += 1;
	}

	public void flush() throws IOException {
		if (bitCount > 0) flushBuffer();
	}

	private void flushBuffer() throws IOException {
		buffer <<= (8 - bitCount);
		writeByte(buffer);
		byteCount++;
		bitCount = 0;
		buffer = 0;
	}

	public int bytesWritten() {
		return byteCount;
	}

	public static OutputBitStream create(final DataOutput output) {
		return new OutputBitStream() {
			@Override
			protected void writeByte(int b) throws IOException {
				output.write(b);
			}
		};
	}

	public static OutputBitStream create(final OutputStream output) {
		return new OutputBitStream() {
			@Override
			protected void writeByte(int b) throws IOException {
				output.write(b);
			}
		};
	}
}
