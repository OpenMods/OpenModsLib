package openmods.utils.io;

import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamAdapters {
	public static IByteSink createSink(final DataOutput output) {
		return new IByteSink() {
			@Override
			public void acceptByte(int b) throws IOException {
				output.write(b);
			}
		};
	}

	public static IByteSink createSink(final OutputStream output) {
		return new IByteSink() {
			@Override
			public void acceptByte(int b) throws IOException {
				output.write(b);
			}
		};
	}

	public static IByteSink createSink(final ByteBuf output) {
		return new IByteSink() {
			@Override
			public void acceptByte(int b) {
				output.writeByte(b);
			}
		};
	}

	public static IByteSource createSource(final DataInput input) {
		return new IByteSource() {
			@Override
			public int nextByte() throws IOException {
				return input.readByte();
			}
		};
	}

	public static IByteSource createSource(final InputStream input) {
		return new IByteSource() {
			@Override
			public int nextByte() throws IOException {
				final int b = input.read();
				if (b < 0) throw new EOFException();
				return b;
			}
		};
	}

	public static IByteSource createSource(final ByteBuf output) {
		return new IByteSource() {
			@Override
			public int nextByte() throws IOException {
				try {
					return output.readUnsignedByte();
				} catch (IndexOutOfBoundsException e) {
					throw new EOFException();
				}
			}
		};
	}

	public static IByteSource createSource(byte[] bytes) {
		return createSource(new ByteArrayInputStream(bytes));
	}
}
