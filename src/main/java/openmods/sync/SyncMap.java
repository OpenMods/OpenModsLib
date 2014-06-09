package openmods.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
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
import com.google.common.collect.*;

import cpw.mods.fml.common.network.ByteBufUtils;

public abstract class SyncMap<H extends ISyncHandler> {

	private static final int MAX_OBJECT_NUM = 16;

	public enum HandlerType {
		TILE_ENTITY {

			@Override
			public ISyncHandler findHandler(World world, DataInput input) throws IOException {
				int x = input.readInt();
				int y = input.readInt();
				int z = input.readInt();
				if (world != null) {
					if (world.blockExists(x, y, z)) {
						TileEntity tile = world.getTileEntity(x, y, z);
						if (tile instanceof ISyncHandler) return (ISyncHandler)tile;
					}
				}

				Log.warn("Invalid handler info: can't find ISyncHandler TE @ (%d,%d,%d)", x, y, z);
				return null;
			}

			@Override
			public void writeHandlerInfo(ISyncHandler handler, DataOutput output) throws IOException {
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
			public ISyncHandler findHandler(World world, DataInput input) throws IOException {
				int entityId = input.readInt();
				Entity entity = world.getEntityByID(entityId);
				if (entity instanceof ISyncHandler)
				return (ISyncHandler)entity;

				Log.warn("Invalid handler info: can't find ISyncHandler entity id %d", entityId);
				return null;
			}

			@Override
			public void writeHandlerInfo(ISyncHandler handler, DataOutput output) throws IOException {
				try {
					Entity e = (Entity)handler;
					output.writeInt(e.getEntityId());
				} catch (ClassCastException e) {
					throw new RuntimeException("Invalid usage of handler type", e);
				}
			}

		};

		public abstract ISyncHandler findHandler(World world, DataInput input) throws IOException;

		public abstract void writeHandlerInfo(ISyncHandler handler, DataOutput output) throws IOException;

		private static final HandlerType[] TYPES = values();
	}

	protected final H handler;

	private Set<Integer> knownUsers = new HashSet<Integer>();

	protected ISyncableObject[] objects = new ISyncableObject[16];
	protected HashMap<String, Integer> nameMap = new HashMap<String, Integer>();

	private int index = 0;

	protected SyncMap(H handler) {
		this.handler = handler;
	}

	public void put(String name, ISyncableObject value) {
		Preconditions.checkState(index < MAX_OBJECT_NUM, "Can't add more than %s objects", MAX_OBJECT_NUM);
		nameMap.put(name, index);
		objects[index++] = value;
	}

	public ISyncableObject get(String name) {
		if (nameMap.containsKey(name)) { return objects[nameMap.get(name)]; }
		return null;
	}

	public int size() {
		return index;
	}

	public Set<ISyncableObject> readFromStream(DataInput dis) throws IOException {
		short mask = dis.readShort();
		Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		int currentBit = 0;

		while (mask != 0) {
			if ((mask & 1) != 0) {
				final ISyncableObject object = objects[currentBit];
				if (object != null) {
					object.readFromStream(dis);
					changes.add(object);
					object.resetChangeTimer(getWorld());
				}
			}
			currentBit++;
			mask >>= 1;
		}
		return changes;
	}

	public int writeToStream(DataOutput dos, boolean regardless) throws IOException {
		int count = 0;
		short mask = 0;
		for (int i = 0; i < index; i++) {
			final ISyncableObject object = objects[i];
			mask = ByteUtils.set(mask, i, object != null
					&& (regardless || object.isDirty()));
		}
		dos.writeShort(mask);
		for (int i = 0; i < index; i++) {
			final ISyncableObject object = objects[i];
			if (object != null && (regardless || object.isDirty())) {
				object.writeToStream(dos, regardless);
				object.resetChangeTimer(getWorld());
				count++;
			}
		}

		return count;
	}

	public void markAllAsClean() {
		for (int i = 0; i < index; i++) {
			if (objects[i] != null) {
				objects[i].markClean();
			}
		}
	}

	protected abstract HandlerType getHandlerType();

	protected abstract Set<EntityPlayerMP> getPlayersWatching();

	protected abstract World getWorld();

	protected abstract boolean isInvalid();

	public Set<ISyncableObject> sync() {
		if (isInvalid()) return ImmutableSet.of();

		Preconditions.checkState(!getWorld().isRemote, "This method can only be used server side");
		Set<EntityPlayerMP> players = getPlayersWatching();
		Set<ISyncableObject> changes = listChanges();
		final boolean hasChanges = !changes.isEmpty();

		List<EntityPlayerMP> fullPacketTargets = Lists.newArrayList();
		List<EntityPlayerMP> deltaPacketTargets = Lists.newArrayList();

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

			if (!fullPacketTargets.isEmpty()) {
				ByteBuf deltaPayload = createPayload(true);
				SyncChannelHolder.INSTANCE.sendPayloadToPlayers(deltaPayload, fullPacketTargets);
			}
		} catch (IOException e) {
			Log.warn(e, "IOError during downstream sync");
		}

		markAllAsClean();
		return changes;
	}

	private Set<ISyncableObject> listChanges() {
		Set<ISyncableObject> changes = Sets.newIdentityHashSet();
		for (ISyncableObject obj : objects) {
			if (obj != null && obj.isDirty()) changes.add(obj);
		}

		return changes;
	}

	public ByteBuf createPayload(boolean fullPacket) throws IOException {
		ByteBuf output = Unpooled.buffer();

		HandlerType type = getHandlerType();
		ByteBufUtils.writeVarInt(output, type.ordinal(), 5);

		DataOutput dataOutput = new ByteBufOutputStream(output);
		type.writeHandlerInfo(handler, dataOutput);
		writeToStream(dataOutput, fullPacket);

		return output;
	}

	public static ISyncHandler findSyncMap(World world, DataInput input) throws IOException {
		int handlerTypeId = ByteUtils.readVLI(input);

		// If this happens, abort! Serious bug!
		Preconditions.checkPositionIndex(handlerTypeId, HandlerType.TYPES.length, "handler type");

		HandlerType handlerType = HandlerType.TYPES[handlerTypeId];

		ISyncHandler handler = handlerType.findHandler(world, input);
		return handler;
	}

	private static final Map<Class<? extends ISyncHandler>, List<Field>> syncedFields = Maps.newIdentityHashMap();

	private static final Comparator<Field> FIELD_NAME_COMPARATOR = new Comparator<Field>() {
		@Override
		public int compare(Field o1, Field o2) {
			// No need to worry about nulls
			return o1.getName().compareTo(o2.getName());
		}
	};

	public void writeToNBT(NBTTagCompound tag) {
		for (Entry<String, Integer> entry : nameMap.entrySet()) {
			int index = entry.getValue();
			String name = entry.getKey();
			if (objects[index] != null) {
				objects[index].writeToNBT(tag, name);
			}
		}
	}

	public void readFromNBT(NBTTagCompound tag) {
		for (Entry<String, Integer> entry : nameMap.entrySet()) {
			int index = entry.getValue();
			String name = entry.getKey();
			if (objects[index] != null) {
				objects[index].readFromNBT(tag, name);
			}
		}
	}

	private static List<Field> getSyncedFields(ISyncHandler handler) {
		Class<? extends ISyncHandler> handlerCls = handler.getClass();
		List<Field> result = syncedFields.get(handlerCls);

		if (result == null) {
			Set<Field> fields = Sets.newTreeSet(FIELD_NAME_COMPARATOR);
			for (Field field : handlerCls.getDeclaredFields()) {
				if (ISyncableObject.class.isAssignableFrom(field.getType())) {
					fields.add(field);
					field.setAccessible(true);
				}
			}
			result = ImmutableList.copyOf(fields);
			syncedFields.put(handlerCls, result);
		}

		return result;
	}

	public void autoregister() {
		for (Field field : getSyncedFields(handler)) {
			try {
				put(field.getName(), (ISyncableObject)field.get(handler));
			} catch (Exception e) {
				Log.severe(e, "Exception while registering synce field '%s'", field);
			}
		}
	}
}
