package openmods.utils.io;

import java.io.*;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedBytes;

public class PacketChunker {

	private byte packetId = 0;

	private final Map<Byte, byte[][]> chunks = Maps.newHashMap();

	public static final int MAX_CHUNK_SIZE = Short.MAX_VALUE - 100;

	public static final int PACKET_SIZE_S3F = 0x001FFFF0;
	public static final int PACKET_SIZE_C17 = 0x00007FFF;

	public byte[][] splitIntoChunks(byte[] data, int maxChunkSize) {

		final int numChunks = (data.length + maxChunkSize - 1) / maxChunkSize;
		Preconditions.checkArgument(numChunks < 256, "%s chunks? Way too much data, man.", numChunks);
		byte[][] result = new byte[numChunks][];

		int chunkOffset = 0;
		for (int chunkIndex = 0; chunkIndex < numChunks; chunkIndex++) {
			// size of the current chunk
			int chunkSize = Math.min(data.length - chunkOffset, maxChunkSize);

			ByteArrayDataOutput buf = ByteStreams.newDataOutput(maxChunkSize);

			buf.writeByte(numChunks);
			if (numChunks > 1) {
				buf.writeByte(chunkIndex);
				buf.writeByte(packetId);
			}

			buf.write(data, chunkOffset, chunkSize);
			result[chunkIndex] = buf.toByteArray();
			chunkOffset += chunkSize;
		}
		packetId++;
		return result;
	}

	public byte[] consumeChunk(byte[] payload) throws IOException {
		return consumeChunk(ByteStreams.newDataInput(payload), payload.length);
	}

	public byte[] consumeChunk(InputStream stream, int payloadLength) throws IOException {
		DataInput data = new DataInputStream(stream);
		return consumeChunk(data, payloadLength);
	}

	/***
	 * Get the bytes from the packet. If the total packet is not yet complete
	 * (and we're waiting for more to complete the sequence), we return null.
	 * Otherwise we return the full byte array
	 *
	 * @param payload
	 *            one of the chunks
	 * @return the full byte array or null if not complete
	 */
	public synchronized byte[] consumeChunk(DataInput input, int payloadLength) throws IOException {
		int numChunks = UnsignedBytes.toInt(input.readByte());

		if (numChunks == 1) {
			byte[] payload = new byte[payloadLength - 1];
			input.readFully(payload);
			return payload;
		}

		int chunkIndex = UnsignedBytes.toInt(input.readByte());
		byte incomingPacketId = input.readByte();

		byte[][] alreadyReceived = chunks.get(incomingPacketId);

		if (alreadyReceived == null) {
			alreadyReceived = new byte[numChunks][];
			chunks.put(incomingPacketId, alreadyReceived);
		}

		byte[] chunkBytes = new byte[payloadLength - 3];
		input.readFully(chunkBytes);

		alreadyReceived[chunkIndex] = chunkBytes;

		for (byte[] s : alreadyReceived)
			if (s == null) return null; // not completed yet

		ByteArrayDataOutput fullPacket = ByteStreams.newDataOutput();

		for (short i = 0; i < numChunks; i++) {
			byte[] chunkPart = alreadyReceived[i];
			fullPacket.write(chunkPart);
		}

		chunks.remove(incomingPacketId);

		return fullPacket.toByteArray();
	}
}
