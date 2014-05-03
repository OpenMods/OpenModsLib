package openmods.utils.io;

import java.util.Arrays;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedBytes;

public class PacketChunker {

	private byte packetId = 0;

	private final Map<Byte, byte[][]> chunks = Maps.newHashMap();

	public static final int MAX_CHUNK_SIZE = Short.MAX_VALUE - 100;

	/***
	 * Split a byte array into one or more chunks with headers
	 * 
	 * @param data
	 * @return the list of chunks
	 */
	public byte[][] splitIntoChunks(byte[] data) {

		final int numChunks = (data.length + MAX_CHUNK_SIZE - 1) / MAX_CHUNK_SIZE;
		Preconditions.checkArgument(numChunks < 256, "%s chunks? Way too much data, man.", numChunks);
		byte[][] result = new byte[numChunks][];

		int chunkOffset = 0;
		for (int chunkIndex = 0; chunkIndex < numChunks; chunkIndex++) {
			// size of the current chunk
			int chunkSize = Math.min(data.length - chunkOffset, MAX_CHUNK_SIZE);

			ByteArrayDataOutput buf = ByteStreams.newDataOutput(MAX_CHUNK_SIZE);

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

	/***
	 * Get the bytes from the packet. If the total packet is not yet complete
	 * (and we're waiting for more to complete the sequence), we return null.
	 * Otherwise we return the full byte array
	 * 
	 * @param payload
	 *            one of the chunks
	 * @return the full byte array or null if not complete
	 */
	public byte[] consumeChunk(byte[] payload) {
		ByteArrayDataInput input = ByteStreams.newDataInput(payload);
		int numChunks = UnsignedBytes.toInt(input.readByte());

		if (numChunks == 1) return Arrays.copyOfRange(payload, 1, payload.length);

		int chunkIndex = UnsignedBytes.toInt(input.readByte());
		byte incomingPacketId = input.readByte();

		byte[][] alreadyReceived = chunks.get(incomingPacketId);

		if (alreadyReceived == null) {
			alreadyReceived = new byte[numChunks][];
			chunks.put(incomingPacketId, alreadyReceived);
		}

		byte[] chunkBytes = new byte[payload.length - 3];
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
