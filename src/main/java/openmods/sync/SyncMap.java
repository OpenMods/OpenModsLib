package openmods.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import openmods.Log;
import openmods.utils.ByteUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.network.ByteBufUtils;

public abstract class SyncMap<H extends ISyncProvider> {

	private static final int MAX_OBJECT_NUM = 16;

	public enum HandlerType {
		TILE_ENTITY {

			@Override
			public ISyncProvider findHandler(World world, DataInput input) throws IOException {
				int x = input.readInt();
				int y = input.readInt();
				int z = input.readInt();
				if (world != null) {
					if (world.blockExists(x, y, z)) {
						TileEntity tile = world.getTileEntity(x, y, z);
						if (tile instanceof ISyncProvider) return (ISyncProvider)tile;
					}
				}

				Log.warn("Invalid handler info: can't find ISyncHandler TE @ (%d,%d,%d)", x, y, z);
				return null;
			}

			@Override
			public void writeHandlerInfo(ISyncProvider handler, DataOutput output) throws IOException {
				try {
					TileEntity te = (TileEntity)handler;
					output.writeInt(te.xCoord);
					output.writeInt(te.yCoord);
					output.writeInt(te.zCoord);
				} catch (ClassCastException e) {
					throw new RuntimeException("Invalid usage of handler type", e);
				}
			}

		},
		ENTITY {

			@Override
			public ISyncProvider findHandler(World world, DataInput input) throws IOException {
				int entityId = input.readInt();
				Entity entity = world.getEntityByID(entityId);
				if (entity instanceof ISyncProvider)
				return (ISyncProvider)entity;

				Log.warn("Invalid handler info: can't find ISyncHandler entity id %d", entityId);
				return null;
			}

			@Override
			public void writeHandlerInfo(ISyncProvider handler, DataOutput output) throws IOException {
				try {
					Entity e = (Entity)handler;
					output.writeInt(e.getEntityId());
				} catch (ClassCastException e) {
					throw new RuntimeException("Invalid usage of handler type", e);
				}
			}

		};

		public abstract ISyncProvider findHandler(World world, DataInput input) throws IOException;

		public abstract void writeHandlerInfo(ISyncProvider handler, DataOutput output) throws IOException;

		private static final HandlerType[] TYPES = values();
	}

	protected final H handler;

	private Set<Integer> knownUsers = Sets.newHashSet();

	protected ISyncableObject[] objects = new ISyncableObject[16];
	protected Map<String, ISyncableObject> nameMap = Maps.newHashMap();
	protected Set<ISyncListener> syncListeners = Sets.newIdentityHashSet();
	protected Set<ISyncListener> updateListeners = Sets.newIdentityHashSet();

	private int index = 0;

	protected SyncMap(H handler) {
		this.handler = handler;
	}

	public void put(String name, ISyncableObject value) {
		Preconditions.checkState(index < MAX_OBJECT_NUM, "Can't add more than %s objects", MAX_OBJECT_NUM);
		objects[index++] = value;
		nameMap.put(name, value);
	}

	public ISyncableObject get(String name) {
		return nameMap.get(name);
	}

	public int size() {
		return index;
	}

	public void readFromStream(DataInput dis) throws IOException {
		short mask = dis.readShort();
		Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		int currentBit = 0;

		while (mask != 0) {
			if ((mask & 1) != 0) {
				final ISyncableObject object = objects[currentBit];
				if (object != null) {
					object.readFromStream(dis);
					changes.add(object);
				}
			}
			currentBit++;
			mask >>= 1;
		}

		if (!changes.isEmpty()) notifySyncListeners(updateListeners, Collections.unmodifiableSet(changes));
	}

	private int writeToStream(DataOutput dos, boolean fullPacket) throws IOException {
		int count = 0;
		short mask = 0;
		for (int i = 0; i < index; i++) {
			final ISyncableObject object = objects[i];
			mask = ByteUtils.set(mask, i, object != null && (fullPacket || object.isDirty()));
		}
		dos.writeShort(mask);
		for (int i = 0; i < index; i++) {
			final ISyncableObject object = objects[i];
			if (object != null && (fullPacket || object.isDirty())) {
				object.writeToStream(dos, fullPacket);
				count++;
			}
		}

		return count;
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
				ByteBuf deltaPayload = createPayload(false);
				SyncChannelHolder.INSTANCE.sendPayloadToPlayers(deltaPayload, deltaPacketTargets);
			}
		} catch (IOException e) {
			Log.warn(e, "IOError during delta sync");
		}

		try {
			if (!fullPacketTargets.isEmpty()) {
				ByteBuf deltaPayload = createPayload(true);
				SyncChannelHolder.INSTANCE.sendPayloadToPlayers(deltaPayload, fullPacketTargets);
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

	public ByteBuf createPayload(boolean fullPacket) throws IOException {
		ByteBuf output = Unpooled.buffer();

		HandlerType type = getHandlerType();
		ByteBufUtils.writeVarInt(output, type.ordinal(), 5);

		DataOutput dataOutput = new ByteBufOutputStream(output);
		type.writeHandlerInfo(handler, dataOutput);
		writeToStream(dataOutput, fullPacket);

		return output.copy();
	}

	public static ISyncProvider findSyncMap(World world, DataInput input) throws IOException {
		int handlerTypeId = ByteUtils.readVLI(input);

		// If this happens, abort! Serious bug!
		Preconditions.checkPositionIndex(handlerTypeId, HandlerType.TYPES.length, "handler type");

		HandlerType handlerType = HandlerType.TYPES[handlerTypeId];

		ISyncProvider handler = handlerType.findHandler(world, input);
		return handler;
	}

	public void writeToNBT(NBTTagCompound tag) {
		for (Entry<String, ISyncableObject> entry : nameMap.entrySet()) {
			final String name = entry.getKey();
			try {
				entry.getValue().writeToNBT(tag, name);
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to read value of field %s", name), e);
			}
		}
	}

	public void readFromNBT(NBTTagCompound tag) {
		for (Entry<String, ISyncableObject> entry : nameMap.entrySet()) {
			String name = entry.getKey();
			try {
				final ISyncableObject obj = entry.getValue();
				obj.readFromNBT(tag, name);
				obj.markClean();
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to read value of field %s", name), e);
			}
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
}
