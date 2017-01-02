package openmods.utils.bitstream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import openmods.utils.io.IByteSource;

public abstract class InputVarBitStream extends InputBitStreamBase {

	private InputVarBitStream() {
		super(0x40);
	}

	public static InputVarBitStream readAll(final IByteSource source) throws IOException {
		final ByteBuf buf = Unpooled.buffer();

		while (true) {
			final int b = source.nextByte();
			buf.writeByte(b);
			if ((b & 0x80) == 0) break;
		}

		buf.readerIndex(0);
		final int size = buf.readableBytes();

		return new InputVarBitStream() {
			@Override
			protected int nextByte() {
				return buf.readUnsignedByte();
			}

			@Override
			public int bytesRead() {
				return size;
			}
		};
	}

	public static InputVarBitStream create(final IByteSource source) {
		return new InputVarBitStream() {
			private int byteCounter;

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
		};
	}
}
