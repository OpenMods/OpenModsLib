package openmods.utils.bitstream;

import java.io.IOException;
import openmods.utils.io.IByteSink;

public class OutputVarBitStream extends OutputBitStream {

	public OutputVarBitStream(IByteSink sink) {
		super(sink, 7);
	}

	@Override
	protected void flushBuffer(int value, boolean isLastBit) throws IOException {
		flushBuffer(value | (isLastBit? 0x00 : 0x80));
	}

}
