package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import openmods.utils.bitstream.InputBitStream;
import openmods.utils.io.IByteSource;

public class SyncMapClient extends SyncMap {

	@Override
	public void read(NBTTagCompound tag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(NBTTagCompound tag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeInitializationData(PacketBuffer dos) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendUpdates() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readIntializationData(PacketBuffer dis) throws IOException {
		final int count = dis.readVarIntFromBuffer();

		final ImmutableList.Builder<ISyncableObject> idToObject = ImmutableList.builder();

		final ImmutableMap.Builder<ISyncableObject, Integer> objectToId = ImmutableMap.builder();

		final Set<ISyncableObject> changedObjects = Sets.newIdentityHashSet();

		for (int i = 0; i < count; i++) {
			final String id = dis.readStringFromBuffer(0xFFFF);
			final int typeId = dis.readVarIntFromBuffer();

			final SyncableObjectType type = SyncableObjectTypeRegistry.getType(typeId);

			ISyncableObject object = availableObjects.get(id);
			if (object == null || !type.isValidType(object))
				object = type.createDummyObject();

			object.readFromStream(dis);

			idToObject.add(object);
			objectToId.put(object, i);

			changedObjects.add(object);
		}

		this.idToObject = idToObject.build();
		this.objectToId = objectToId.build();
		this.bitmapLength = (count + 7) / 8;

		notifySyncListeners(updateListeners, Collections.unmodifiableSet(changedObjects));
	}

	protected final Set<ISyncListener> updateListeners = Sets.newIdentityHashSet();

	@Override
	public void readUpdate(PacketBuffer dis) throws IOException {
		if (bitmapLength <= 0) {
			// Initial data not received yet - assuming this is initialization packet
			readIntializationData(dis);
			return;
		}

		final ByteBuf bitmapData = dis.readSlice(bitmapLength);

		InputBitStream bitmap = new InputBitStream(new IByteSource() {
			@Override
			public int nextByte() {
				return bitmapData.readUnsignedByte();
			}
		});

		final Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		for (int i = 0; i < idToObject.size(); i++) {
			if (bitmap.readBit()) {
				ISyncableObject obj = idToObject.get(i);
				obj.readFromStream(dis);
				changes.add(obj);
			}
		}

		if (!changes.isEmpty())
			notifySyncListeners(updateListeners, Collections.unmodifiableSet(changes));
	}

	private final Map<String, ISyncableObject> availableObjects = Maps.newHashMap();

	private int bitmapLength = 0;

	private List<ISyncableObject> idToObject;

	private Map<ISyncableObject, Integer> objectToId;

	@Override
	public void registerObject(String name, ISyncableObject value) {
		final ISyncableObject prev = availableObjects.put(name, value);
		Preconditions.checkState(prev == null, "Duplicate object '%s': %s -> %s", name, value);
	}

	@Override
	public ISyncableObject getObjectById(int id) {
		Preconditions.checkState(idToObject != null, "Initial data not received yet!");
		return idToObject.get(id);
	}

	@Override
	public int getObjectId(ISyncableObject object) {
		Preconditions.checkState(objectToId != null, "Initial data not received yet!");
		return objectToId.get(object);
	}

	@Override
	public void addSyncListener(ISyncListener listener) {
		// NO-OP
	}

	@Override
	public void addUpdateListener(ISyncListener listener) {
		updateListeners.add(listener);
	}

	@Override
	public void removeUpdateListener(ISyncListener listener) {
		updateListeners.remove(listener);
	}

}
