package openmods.utils.io;

import java.io.EOFException;
import java.io.IOException;

public interface IByteSource {
	public int nextByte() throws IOException, EOFException;
}
