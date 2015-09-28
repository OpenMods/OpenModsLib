package openmods.utils.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class BitStreamsTest {

	public static final byte b00001010 = (byte)0x0A;
	public static final byte b00000000 = (byte)0x00;
	public static final byte b01010101 = (byte)0x55;
	public static final byte b10000000 = (byte)0x80;
	public static final byte b00000001 = (byte)0x01;
	public static final byte b10101010 = (byte)0xAA;
	public static final byte b11111111 = (byte)0xFF;
	public static final byte b10100000 = (byte)0xA0;
	private static final byte b01000000 = (byte)0x40;

	public static void checkInputStream(InputBitStream stream, boolean... bits) throws IOException {
		int bitCount = 0;
		for (boolean bit : bits) {
			Assert.assertEquals("Bit " + bitCount, bit, stream.readBit());
			bitCount++;
		}
	}

	public static void writeBits(OutputBitStream stream, boolean... bits) throws IOException {
		for (boolean bit : bits)
			stream.writeBit(bit);
	}

	public static InputBitStream createInputStream(byte[] bytes) {
		ByteArrayInputStream input = new ByteArrayInputStream(bytes);
		return InputBitStream.create(input);
	}

	public static void checkInputOutput(int size, boolean[] bits) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputBitStream outputStream = OutputBitStream.create(output);

		for (boolean bit : bits)
			outputStream.writeBit(bit);

		outputStream.flush();

		final byte[] bytes = output.toByteArray();
		Assert.assertEquals(size, bytes.length);
		ByteArrayInputStream input = new ByteArrayInputStream(bytes);
		InputBitStream inputStream = InputBitStream.create(input);

		checkInputStream(inputStream, bits);
	}

	@Test
	public void testInputStreamOneByteAllZeros() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { 0 });
		checkInputStream(inputStream, false, false, false, false, false, false, false, false);
	}

	@Test
	public void testInputStreamTwoBytesAllZeros() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { 0, 0 });
		checkInputStream(inputStream,
				false, false, false, false, false, false, false, false,
				false, false, false, false, false, false, false, false);
	}

	@Test
	public void testInputStreamOneByteAllOnes() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b11111111 });
		checkInputStream(inputStream,
				true, true, true, true, true, true, true, true);
	}

	@Test
	public void testInputStreamTwoBytesAllOnes() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b11111111, b11111111 });
		checkInputStream(inputStream,
				true, true, true, true, true, true, true, true,
				true, true, true, true, true, true, true, true);
	}

	@Test
	public void testInputStreamOneByteToggling() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b01010101 });
		checkInputStream(inputStream,
				false, true, false, true, false, true, false, true);
	}

	@Test
	public void testInputStreamTwoBytesToggling() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b01010101, b10101010 });
		checkInputStream(inputStream,
				false, true, false, true, false, true, false, true,
				true, false, true, false, true, false, true, false);
	}

	@Test
	public void testInputStreamOneByteFirstOne() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b10000000 });
		checkInputStream(inputStream,
				true, false, false, false, false, false, false, false);
	}

	@Test
	public void testInputStreamOneByteLastOne() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b00000001 });
		checkInputStream(inputStream,
				false, false, false, false, false, false, false, true);
	}

	@Test
	public void testInputStreamByteCount() throws IOException {
		InputBitStream inputStream = createInputStream(new byte[] { b11111111, b00000000 });
		Assert.assertEquals(0, inputStream.bytesRead());

		for (int i = 0; i < 8; i++) {
			Assert.assertTrue(inputStream.readBit());
			Assert.assertEquals(1, inputStream.bytesRead());
		}

		for (int i = 0; i < 8; i++) {
			Assert.assertFalse(inputStream.readBit());
			Assert.assertEquals(2, inputStream.bytesRead());
		}
	}

	@Test
	public void testOutputEmptyFlush() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputBitStream stream = OutputBitStream.create(output);

		Assert.assertEquals(0, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[0], output.toByteArray());

		stream.flush();

		Assert.assertEquals(0, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[0], output.toByteArray());
	}

	@Test
	public void testOutputBasicOperations() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputBitStream stream = OutputBitStream.create(output);

		for (int i = 0; i < 8; i++) {

			Assert.assertEquals(0, stream.bytesWritten());
			Assert.assertEquals(0, output.toByteArray().length);

			stream.writeBit(true);

			Assert.assertEquals(0, stream.bytesWritten());
			Assert.assertEquals(0, output.toByteArray().length);
		}

		stream.writeBit(true);
		Assert.assertEquals(1, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b11111111 }, output.toByteArray());

		stream.flush();
		Assert.assertEquals(2, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b11111111, b10000000 }, output.toByteArray());

		stream.flush();
		Assert.assertEquals(2, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b11111111, b10000000 }, output.toByteArray());

		stream.writeBit(false);

		stream.flush();
		Assert.assertEquals(3, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b11111111, b10000000, b00000000 }, output.toByteArray());
	}

	@Test
	public void testOutputSingleBitFlush() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputBitStream stream = OutputBitStream.create(output);

		Assert.assertEquals(0, stream.bytesWritten());
		Assert.assertEquals(0, output.toByteArray().length);

		stream.writeBit(true);

		Assert.assertEquals(0, stream.bytesWritten());
		Assert.assertEquals(0, output.toByteArray().length);

		stream.flush();
		Assert.assertEquals(1, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b10000000 }, output.toByteArray());
	}

	@Test
	public void testOutputTwoBitFlush() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputBitStream stream = OutputBitStream.create(output);

		Assert.assertEquals(0, stream.bytesWritten());
		Assert.assertEquals(0, output.toByteArray().length);

		stream.writeBit(false);
		stream.writeBit(true);

		Assert.assertEquals(0, stream.bytesWritten());
		Assert.assertEquals(0, output.toByteArray().length);

		stream.flush();
		Assert.assertEquals(1, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b01000000 }, output.toByteArray());
	}

	@Test
	public void testOutputFourAlternatingBits() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputBitStream stream = OutputBitStream.create(output);

		writeBits(stream, true, false, true, false);

		stream.flush();
		Assert.assertEquals(1, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b10100000 }, output.toByteArray());

		stream.flush();
		Assert.assertEquals(1, stream.bytesWritten());
		Assert.assertArrayEquals(new byte[] { b10100000 }, output.toByteArray());
	}

	@Test
	public void testInputOutput() throws IOException {
		checkInputOutput(0, new boolean[0]);
		checkInputOutput(1, new boolean[] { false });
		checkInputOutput(1, new boolean[] { true });

		checkInputOutput(1, new boolean[] { false, true });
		checkInputOutput(1, new boolean[] { true, false });

		checkInputOutput(1, new boolean[] { true, true, true, true, true, true, true, true });
		checkInputOutput(2, new boolean[] { true, true, true, true, true, true, true, true, true });

		checkInputOutput(1, new boolean[] { false, false, false, false, false, false, false, false });
		checkInputOutput(2, new boolean[] { false, false, false, false, false, false, false, false, false });

		checkInputOutput(1, new boolean[] { true, false, false, false });
		checkInputOutput(1, new boolean[] { false, false, false, true });

		checkInputOutput(2, new boolean[] {
				true, false, false, false, false, false, false, false,
				false, false, false, false });

		checkInputOutput(2, new boolean[] {
				false, false, false, false, false, false, false, false,
				false, false, false, true });

		checkInputOutput(2, new boolean[] {
				true, false, true, false, true, false, true, false,
				false, true, false, true });

		checkInputOutput(2, new boolean[] {
				true, true, true, true, true, true, true, true,
				false, false, false, false, false, false, false, false });
	}
}
