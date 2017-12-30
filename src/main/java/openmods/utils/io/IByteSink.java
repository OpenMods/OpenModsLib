package openmods.utils.io;

import java.io.IOException;

@FunctionalInterface
public interface IByteSink {
	public void acceptByte(int b) throws IOException;
}
