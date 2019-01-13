package openmods.utils.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import openmods.utils.bitstream.InputVarBitStream;
import openmods.utils.bitstream.OutputVarBitStream;
import org.junit.Assert;
import org.junit.Test;

public class VarBitStreamsTest {

	public static final byte b00000000 = (byte)0x00;
	public static final byte b10000000 = (byte)0x80;

	public static final byte b01000001 = (byte)0x41;

	public static final byte b01000000 = (byte)0x40;
	public static final byte b10000001 = (byte)0x81;

	public static void checkInputStream(InputVarBitStream stream, boolean... bits) throws IOException {
		int bitCount = 0;
		for (boolean bit : bits) {
			Assert.assertEquals("Bit " + bitCount, bit, stream.readBit());
			bitCount++;
		}
	}

	public static InputVarBitStream createInputStream(byte... bytes) throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(bytes);
		final InputVarBitStream result = InputVarBitStream.readAll(StreamAdapters.createSource(input));
		Assert.assertEquals(bytes.length, result.bytesRead());
		Assert.assertEquals(input.read(), -1);
		return result;
	}

	private static OutputVarBitStream createOutputStream(ByteArrayOutputStream output) {
		return new OutputVarBitStream(StreamAdapters.createSink(output));
	}

	public static void writeBits(OutputVarBitStream stream, boolean... bits) throws IOException {
		for (boolean bit : bits)
			stream.writeBit(bit);
	}

	private static void checkOutput(OutputVarBitStream stream, ByteArrayOutputStream output, byte... bytes) {
		Assert.assertEquals(bytes.length, stream.bytesWritten());
		Assert.assertArrayEquals(bytes, output.toByteArray());
	}

	@Test
	public void testAllZerosInput() throws IOException {
		InputVarBitStream inputStream = createInputStream(b00000000);

		checkInputStream(inputStream, false, false, false, false, false, false, false);
	}

	@Test
	public void testTwoContinuedZerosInput() throws IOException {
		InputVarBitStream inputStream = createInputStream(b10000000, b00000000);
		checkInputStream(inputStream,
				false, false, false, false, false, false, false,
				false, false, false, false, false, false, false);
	}

	@Test
	public void testThreeContinuedZerosInput() throws IOException {
		InputVarBitStream inputStream = createInputStream(b10000000, b10000000, b00000000);
		checkInputStream(inputStream,
				false, false, false, false, false, false, false,
				false, false, false, false, false, false, false,
				false, false, false, false, false, false, false);
	}

	@Test
	public void testOneBytesFirstLastOnesInput() throws IOException {
		InputVarBitStream inputStream = createInputStream(b01000001);
		checkInputStream(inputStream,
				true, false, false, false, false, false, true);
	}

	@Test
	public void testSevenZerosOutput() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputVarBitStream stream = createOutputStream(output);

		writeBits(stream, false, false, false, false, false, false, false);
		Assert.assertEquals(0, stream.bytesWritten());

		stream.flush();

		checkOutput(stream, output, b00000000);
	}

	@Test
	public void testEightZerosOutput() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputVarBitStream stream = createOutputStream(output);

		writeBits(stream, false, false, false, false, false, false, false);
		writeBits(stream, false);
		checkOutput(stream, output, b10000000);

		stream.flush();
		checkOutput(stream, output, b10000000, b00000000);
	}

	@Test
	public void testFifteenZerosOutput() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputVarBitStream stream = createOutputStream(output);

		writeBits(stream, false, false, false, false, false, false, false, false);
		writeBits(stream, false, false, false, false, false, false, false, false);
		writeBits(stream, false);

		checkOutput(stream, output, b10000000, b10000000);

		stream.flush();
		checkOutput(stream, output, b10000000, b10000000, b00000000);
	}

	@Test
	public void testDirectionAndOrderInput() throws IOException {
		InputVarBitStream inputStream = createInputStream(b10000001, b01000000);
		checkInputStream(inputStream,
				false, false, false, false, false, false, true,
				true, false, false, false, false, false, false);
	}

	@Test
	public void testDirectionAndOrderOutput() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputVarBitStream stream = createOutputStream(output);

		writeBits(stream,
				false, false, false, false, false, false, true,
				true, false, false, false, false, false, false);
		stream.flush();

		checkOutput(stream, output, b10000001, b01000000);
	}
}
