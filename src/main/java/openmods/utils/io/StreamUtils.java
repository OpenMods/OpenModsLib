package openmods.utils.io;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

	public static class EndOfStreamException extends RuntimeException {
		private static final long serialVersionUID = -3617549679310092671L;
	}

	public static int bitsToBytes(int bits) {
		return (bits + 7) / 8;
	}

	public static byte[] readBytes(InputStream stream, int count) throws IOException {
		byte[] buffer = new byte[count];
		int read = stream.read(buffer);
		if (read != count) throw new EndOfStreamException();
		return buffer;
	}

	public static byte[] readBytes(DataInput stream, int count) throws IOException {
		byte[] buffer = new byte[count];
		try {
			stream.readFully(buffer);
		} catch (EOFException e) {
			throw new EndOfStreamException();
		}

		return buffer;
	}
}
