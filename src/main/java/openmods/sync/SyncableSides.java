package openmods.sync;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import openmods.utils.ByteUtils;
import openmods.utils.DirUtils;
import openmods.utils.bitmap.IBitMap;
import openmods.utils.bitmap.IRpcDirectionBitMap;

import com.google.common.collect.Iterators;

public class SyncableSides extends SyncableObjectBase implements IRpcDirectionBitMap, IBitMap<EnumFacing>, ISyncableValueProvider<Set<EnumFacing>> {

	private Set<EnumFacing> dirs = EnumSet.noneOf(EnumFacing.class);

	private void read(int bits) {
		dirs.clear();
		Iterators.addAll(dirs, DirUtils.bitsToValidDirs(bits));
	}

	private int write() {
		return ByteUtils.enumSetToBits(dirs);
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		read(stream.readByte());
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeByte(write());
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setByte(name, (byte)write());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		read(nbt.getByte(name));
	}

	@Override
	public Set<EnumFacing> getValue() {
		return Collections.unmodifiableSet(dirs);
	}

	@Override
	public void mark(EnumFacing dir) {
		if (dirs.add(dir)) markDirty();
	}

	@Override
	public void clear(EnumFacing dir) {
		if (dirs.remove(dir)) markDirty();
	}

	@Override
	public boolean get(EnumFacing dir) {
		return dirs.contains(dir);
	}

	@Override
	public void clearAll() {
		dirs.clear();
		markDirty();
	}

	@Override
	public void toggle(EnumFacing value) {
		if (!dirs.remove(value)) dirs.add(value);
		markDirty();
	}

	@Override
	public void set(EnumFacing key, boolean value) {
		if (value) dirs.add(key);
		else dirs.remove(key);
		markDirty();
	}
}
