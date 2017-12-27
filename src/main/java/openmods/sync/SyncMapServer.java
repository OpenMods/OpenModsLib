package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import openmods.Log;
import openmods.utils.bitstream.OutputBitStream;
import openmods.utils.io.IByteSink;

public abstract class SyncMapServer extends SyncMap {

	private final Map<String, ISyncableObject> objects = Maps.newHashMap();

	private static class Entry {
		private final String name;
		private final ISyncableObject obj;
		private final SyncableObjectType type;

		public Entry(String name, ISyncableObject obj) {
			this.name = name;
			this.obj = obj;
			this.type = SyncableObjectTypeRegistry.getType(obj.getClass());
			Preconditions.checkNotNull(type, "Type %s is not registered", obj.getClass());
		}
	}

	private final List<Entry> orderedEntries = Lists.newArrayList();

	private final Map<ISyncableObject, Integer> objectToId = Maps.newIdentityHashMap();

	private boolean firstRemoteObjectInitialized = false;

	private int bitmapLength;

	private final IUpdateStrategy updateStrategy;

	public SyncMapServer(UpdateStrategy strategy) {
		this.updateStrategy = strategy.create(this);
	}

	@Override
	public void registerObject(String name, ISyncableObject value) {
		Preconditions.checkState(!firstRemoteObjectInitialized, "Can't add fields to object that has already sent data to clients");

		{
			final ISyncableObject prev = objects.put(name, value);
			Preconditions.checkState(prev == null, "Duplicate name '%s', %s -> %s", name, prev, value);
		}

		final int newId = orderedEntries.size();
		orderedEntries.add(new Entry(name, value));

		{
			final Integer prev = objectToId.put(value, newId);
			Preconditions.checkState(prev == null, "Duplicate object '%s', %s -> %s", name, prev, newId);
		}
	}

	@Override
	public void read(NBTTagCompound tag) {
		for (Map.Entry<String, ISyncableObject> entry : objects.entrySet()) {
			String name = entry.getKey();
			final ISyncableObject obj = entry.getValue();
			try {
				obj.readFromNBT(tag, name);
			} catch (Throwable e) {
				throw new SyncFieldException(e, name);
			}
			obj.markClean();
		}
	}

	@Override
	public boolean tryRead(NBTTagCompound tag) {
		read(tag);
		return true;
	}

	@Override
	public void write(NBTTagCompound tag) {
		for (Map.Entry<String, ISyncableObject> entry : objects.entrySet()) {
			final String name = entry.getKey();
			final ISyncableObject obj = entry.getValue();
			try {
				obj.writeToNBT(tag, name);
			} catch (Throwable e) {
				throw new SyncFieldException(e, name);
			}
		}
	}

	@Override
	public boolean tryWrite(NBTTagCompound tag) {
		write(tag);
		return true;
	}

	@Override
	public void readIntializationData(PacketBuffer dis) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readUpdate(PacketBuffer dis) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeInitializationData(PacketBuffer dos) throws IOException {
		updateStrategy.writeInitializationData(dos);
	}

	private void writeOwnerInfo(PacketBuffer dos) {
		dos.writeVarInt(getOwnerType());
		writeOwnerData(dos);
	}

	private void writeSyncObjectInitialization(PacketBuffer dos) throws IOException {
		if (!firstRemoteObjectInitialized) {
			firstRemoteObjectInitialized = true;
			bitmapLength = (objects.size() + 7) / 8;
		}

		dos.writeVarInt(objects.size());

		for (Entry e : orderedEntries) {
			dos.writeString(e.name);

			final int typeId = SyncableObjectTypeRegistry.getTypeId(e.type);
			dos.writeVarInt(typeId);

			e.obj.writeToStream(dos);
		}
	}

	private void writeUpdatePacket(PacketBuffer dos, Set<ISyncableObject> changes) throws IOException {
		Preconditions.checkState(firstRemoteObjectInitialized, "Remote objects not intialized yet");

		final ByteBuf bitmapData = dos.slice(dos.writerIndex(), bitmapLength);
		bitmapData.clear();
		dos.writeZero(bitmapLength);

		final OutputBitStream bitmap = new OutputBitStream(new IByteSink() {
			@Override
			public void acceptByte(int b) {
				bitmapData.writeByte(b);
			}
		});

		for (Entry e : orderedEntries) {
			if (changes.contains(e.obj)) {
				e.obj.writeToStream(dos);
				bitmap.writeBit(true);
			} else {
				bitmap.writeBit(false);
			}
		}

		bitmap.flush();
	}

	protected interface IUpdateStrategy {
		public void sendUpdates(Set<ISyncableObject> changedObjects);

		public void writeInitializationData(PacketBuffer dos) throws IOException;

		public boolean canSendUpdates();
	}

	private class SeparateInitializationPacketStrategy implements IUpdateStrategy {

		@Override
		public void sendUpdates(Set<ISyncableObject> changedObjects) {
			if (changedObjects.isEmpty()) return;

			final Set<EntityPlayerMP> players = getPlayersWatching();

			try {
				final PacketBuffer deltaPayload = new PacketBuffer(Unpooled.buffer());
				writeOwnerInfo(deltaPayload);
				writeUpdatePacket(deltaPayload, changedObjects);
				SyncChannelHolder.INSTANCE.sendPayloadToPlayers(deltaPayload, players);
			} catch (IOException e) {
				Log.warn(e, "IOError during delta sync");
			}
		}

		@Override
		public void writeInitializationData(PacketBuffer dos) throws IOException {
			// owner info not required, as initialization packet is assumed to already be directed
			writeSyncObjectInitialization(dos);
		}

		@Override
		public boolean canSendUpdates() {
			return firstRemoteObjectInitialized;
		}
	}

	private class SelfInitializingUpdateStrategy implements IUpdateStrategy {

		private Set<Integer> knownUsers = Sets.newHashSet();

		@Override
		public void sendUpdates(Set<ISyncableObject> changes) {
			final boolean hasChanges = !changes.isEmpty();

			List<EntityPlayerMP> fullPacketTargets = Lists.newArrayList();
			List<EntityPlayerMP> deltaPacketTargets = Lists.newArrayList();

			Set<EntityPlayerMP> players = getPlayersWatching();
			for (EntityPlayerMP player : players) {
				if (knownUsers.contains(player.getEntityId())) {
					if (hasChanges) deltaPacketTargets.add(player);
				} else {
					knownUsers.add(player.getEntityId());
					fullPacketTargets.add(player);
				}
			}

			try {
				if (!deltaPacketTargets.isEmpty()) {
					final PacketBuffer deltaPayload = new PacketBuffer(Unpooled.buffer());
					writeOwnerInfo(deltaPayload);
					writeUpdatePacket(deltaPayload, changes);
					SyncChannelHolder.INSTANCE.sendPayloadToPlayers(deltaPayload, deltaPacketTargets);
				}
			} catch (IOException e) {
				Log.warn(e, "IOError during delta sync");
			}

			try {
				if (!fullPacketTargets.isEmpty()) {
					final PacketBuffer fullPayload = new PacketBuffer(Unpooled.buffer());
					writeOwnerInfo(fullPayload);
					writeSyncObjectInitialization(fullPayload);
					SyncChannelHolder.INSTANCE.sendPayloadToPlayers(fullPayload, fullPacketTargets);
				}
			} catch (IOException e) {
				Log.warn(e, "IOError during full sync");
			}
		}

		@Override
		public void writeInitializationData(PacketBuffer dos) {
			// use other strategy, if you want to send update packet
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canSendUpdates() {
			return true;
		}
	}

	public enum UpdateStrategy {
		WITHOUT_INITIAL_PACKET {
			@Override
			protected IUpdateStrategy create(SyncMapServer owner) {
				return owner.new SelfInitializingUpdateStrategy();
			}
		},
		WITH_INITIAL_PACKET {
			@Override
			protected IUpdateStrategy create(SyncMapServer owner) {
				return owner.new SeparateInitializationPacketStrategy();
			}
		};

		protected abstract IUpdateStrategy create(SyncMapServer owner);
	}

	private Set<ISyncableObject> listChanges() {
		Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		for (Entry e : orderedEntries)
			if (e.obj.isDirty()) {
				changes.add(e.obj);
				e.obj.markClean();
			}

		return changes;
	}

	protected final Set<ISyncListener> syncListeners = Sets.newIdentityHashSet();

	@Override
	public void addSyncListener(ISyncListener listener) {
		syncListeners.add(listener);
	}

	@Override
	public void addUpdateListener(ISyncListener listener) {
		// NO-OP
	}

	@Override
	public void removeUpdateListener(ISyncListener dispatcher) {
		// NO-OP
	}

	@Override
	public void sendUpdates() {
		if (isInvalid() || !updateStrategy.canSendUpdates()) return;

		final Set<ISyncableObject> changedObjects = listChanges();
		updateStrategy.sendUpdates(changedObjects);

		if (!changedObjects.isEmpty()) {
			notifySyncListeners(syncListeners, Collections.unmodifiableSet(changedObjects));
		}
	}

	@Override
	public boolean trySendUpdates() {
		sendUpdates();
		return true;
	}

	@Override
	public ISyncableObject getObjectById(int objectId) {
		try {
			return orderedEntries.get(objectId).obj;
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException(Integer.toString(objectId));
		}
	}

	@Override
	public int getObjectId(ISyncableObject object) {
		Integer result = objectToId.get(object);
		if (result == null) throw new NoSuchElementException(String.valueOf(object));
		return result;
	}

	protected abstract int getOwnerType();

	protected abstract void writeOwnerData(PacketBuffer output);

	protected abstract Set<EntityPlayerMP> getPlayersWatching();

	protected abstract boolean isInvalid();
}
