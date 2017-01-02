package openmods.utils.io;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

public abstract class InputBitStream {

	protected abstract int nextByte() throws IOException;

	private int byteCounter;

	private int mask;

	private int currentByte;

	public boolean readBit() throws IOException {
		if (mask == 0) {
			currentByte = nextByte();
			byteCounter++;
			mask = 0x80;
		}

		boolean bit = (currentByte & mask) != 0;
		mask >>= 1;

		return bit;
	}

	public int bytesRead() {
		return byteCounter;
	}

	public static InputBitStream create(final DataInput input) {
		return new InputBitStream() {
			@Override
			protected int nextByte() throws IOException {
				return input.readByte();
			}
		};
	}

	public static InputBitStream create(final InputStream input) {
		return new InputBitStream() {
			@Override
			protected int nextByte() throws IOException {
				return input.read();
			}
		};
	}

	public static InputBitStream create(byte[] bytes) {
		return create(new ByteArrayInputStream(bytes));
	}
}
