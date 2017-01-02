package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import openmods.Log;
import openmods.utils.ByteUtils;

public abstract class SyncMap<H extends ISyncMapProvider> {

	public static class SyncFieldException extends RuntimeException {
		private static final long serialVersionUID = -3154521464407191767L;

		public SyncFieldException(Throwable cause, String name) {
			super(String.format("Failed to sync field '%s'", name), cause);
		}

		public SyncFieldException(Throwable cause, int index) {
			super(String.format("Failed to sync field #%d", index), cause);
		}
	}

	private static final int MAX_OBJECT_NUM = Short.SIZE;

	public enum HandlerType {
		TILE_ENTITY {

			@Override
			public ISyncMapProvider findHandler(World world, PacketBuffer input) {
				final BlockPos pos = input.readBlockPos();
				if (world != null) {
					if (world.isBlockLoaded(pos)) {
						final TileEntity tile = world.getTileEntity(pos);
						if (tile instanceof ISyncMapProvider) return (ISyncMapProvider)tile;
					}
				}

				return null;
			}

			@Override
			public void writeHandlerInfo(ISyncMapProvider handler, PacketBuffer output) {
				try {
					final TileEntity te = (TileEntity)handler;
					output.writeBlockPos(te.getPos());
				} catch (ClassCastException e) {
					throw new RuntimeException("Invalid usage of handler type", e);
				}
			}

		},
		ENTITY {

			@Override
			public ISyncMapProvider findHandler(World world, PacketBuffer input) {
				int entityId = input.readInt();
				Entity entity = world.getEntityByID(entityId);
				if (entity instanceof ISyncMapProvider)
					return (ISyncMapProvider)entity;

				Log.warn("Invalid handler info: can't find ISyncHandler entity id %d", entityId);
				return null;
			}

			@Override
			public void writeHandlerInfo(ISyncMapProvider handler, PacketBuffer output) {
				try {
					Entity e = (Entity)handler;
					output.writeInt(e.getEntityId());
				} catch (ClassCastException e) {
					throw new RuntimeException("Invalid usage of handler type", e);
				}
			}

		};

		public abstract ISyncMapProvider findHandler(World world, PacketBuffer input);

		public abstract void writeHandlerInfo(ISyncMapProvider handler, PacketBuffer output) throws IOException;

		private static final HandlerType[] TYPES = values();
	}

	protected final H handler;

	private Set<Integer> knownUsers = Sets.newHashSet();

	private ISyncableObject[] objects = new ISyncableObject[16];
	private Map<String, ISyncableObject> nameMap = Maps.newHashMap();
	private Map<ISyncableObject, Integer> objectToId = Maps.newIdentityHashMap();

	private Set<ISyncListener> syncListeners = Sets.newIdentityHashSet();
	private Set<ISyncListener> updateListeners = Sets.newIdentityHashSet();

	private int index = 0;

	protected SyncMap(H handler) {
		this.handler = handler;
	}

	public void put(String name, ISyncableObject value) {
		Preconditions.checkState(index < MAX_OBJECT_NUM, "Can't add more than %s objects", MAX_OBJECT_NUM);
		int objId = index++;
		objects[objId] = value;
		nameMap.put(name, value);
		Integer prev = objectToId.put(value, objId);
		Preconditions.checkState(prev == null, "Object %s registered twice, under ids %s and %s", value, prev, objId);
	}

	public ISyncableObject get(String name) {
		ISyncableObject result = nameMap.get(name);
		if (result == null) throw new NoSuchElementException(name);
		return result;
	}

	public ISyncableObject get(int objectId) {
		try {
			return objects[objectId];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException(Integer.toString(objectId));
		}
	}

	public int getId(ISyncableObject object) {
		Integer result = objectToId.get(object);
		if (result == null) throw new NoSuchElementException(String.valueOf(object));
		return result;
	}

	public int size() {
		return index;
	}

	public void readFromStream(PacketBuffer dis) {
		int mask = dis.readShort();
		Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		int currentBit = 0;

		while (mask != 0) {
			if ((mask & 1) != 0) {
				final ISyncableObject object = objects[currentBit];
				if (object != null) {
					try {
						object.readFromStream(dis);
					} catch (Throwable t) {
						throw new SyncFieldException(t, currentBit);
					}
					changes.add(object);
				}
			}
			currentBit++;
			mask >>= 1;
		}

		if (!changes.isEmpty()) notifySyncListeners(updateListeners, Collections.unmodifiableSet(changes));
	}

	private void writeToStream(PacketBuffer dos, boolean fullPacket) {
		int mask = 0;
		for (int i = 0; i < index; i++) {
			final ISyncableObject object = objects[i];
			if (object != null && (fullPacket || object.isDirty())) {
				mask = ByteUtils.on(mask, i);
			}
		}
		dos.writeShort(mask);

		for (int i = 0; i < index; i++) {
			final ISyncableObject object = objects[i];
			if (object != null && (fullPacket || object.isDirty())) {
				try {
					object.writeToStream(dos);
				} catch (Throwable t) {
					throw new SyncFieldException(t, i);
				}
			}
		}
	}

	protected abstract HandlerType getHandlerType();

	protected abstract Set<EntityPlayerMP> getPlayersWatching();

	protected abstract World getWorld();

	protected abstract boolean isInvalid();

	public void sync() {
		Preconditions.checkState(!getWorld().isRemote, "This method can only be used server side");
		if (isInvalid()) return;

		Set<ISyncableObject> changes = listChanges();
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
				final PacketBuffer deltaPayload = createPayload(false);
				SyncChannelHolder.INSTANCE.sendPayloadToPlayers(deltaPayload, deltaPacketTargets);
			}
		} catch (IOException e) {
			Log.warn(e, "IOError during delta sync");
		}

		try {
			if (!fullPacketTargets.isEmpty()) {
				final PacketBuffer fullPayload = createPayload(true);
				SyncChannelHolder.INSTANCE.sendPayloadToPlayers(fullPayload, fullPacketTargets);
			}
		} catch (IOException e) {
			Log.warn(e, "IOError during full sync");
		}

		if (hasChanges) {
			unmarkChanges(changes);
			notifySyncListeners(syncListeners, Collections.unmodifiableSet(changes));
		}
	}

	private Set<ISyncableObject> listChanges() {
		Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		for (int i = 0; i < index; i++) {
			ISyncableObject obj = objects[i];
			if (obj != null && obj.isDirty()) changes.add(obj);
		}

		return changes;
	}

	private static void unmarkChanges(Set<ISyncableObject> changes) {
		for (ISyncableObject obj : changes)
			obj.markClean();
	}

	public PacketBuffer createPayload(boolean fullPacket) throws IOException {
		final PacketBuffer output = new PacketBuffer(Unpooled.buffer());

		final HandlerType type = getHandlerType();
		output.writeVarIntToBuffer(type.ordinal());

		type.writeHandlerInfo(handler, output);
		writeToStream(output, fullPacket);

		return output;
	}

	public static ISyncMapProvider findSyncMap(World world, PacketBuffer input) {
		final int handlerTypeId = input.readVarIntFromBuffer();

		// If this happens, abort! Serious bug!
		Preconditions.checkPositionIndex(handlerTypeId, HandlerType.TYPES.length, "handler type");

		final HandlerType handlerType = HandlerType.TYPES[handlerTypeId];

		return handlerType.findHandler(world, input);
	}

	public void writeToNBT(NBTTagCompound tag) {
		for (Entry<String, ISyncableObject> entry : nameMap.entrySet()) {
			final String name = entry.getKey();
			final ISyncableObject obj = entry.getValue();
			try {
				obj.writeToNBT(tag, name);
			} catch (Throwable e) {
				throw new SyncFieldException(e, name);
			}
		}
	}

	public void readFromNBT(NBTTagCompound tag) {
		for (Entry<String, ISyncableObject> entry : nameMap.entrySet()) {
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

	private static void notifySyncListeners(Collection<ISyncListener> listeners, Set<ISyncableObject> allChanges) {
		for (ISyncListener listener : listeners)
			listener.onSync(allChanges);
	}

	public void addSyncListener(ISyncListener listener) {
		syncListeners.add(listener);
	}

	public void addUpdateListener(ISyncListener listener) {
		updateListeners.add(listener);
	}

	public void removeUpdateListener(ISyncListener dispatcher) {
		updateListeners.remove(dispatcher);
	}
}
