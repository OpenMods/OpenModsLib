package openmods.utils.io;

import java.io.IOException;

@FunctionalInterface
public interface IByteSource {
	int nextByte() throws IOException;
}
