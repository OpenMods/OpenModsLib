package openmods.sync;

import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import openmods.utils.ByteUtils;
import openmods.utils.DirUtils;
import openmods.utils.bitmap.IBitMap;
import openmods.utils.bitmap.IRpcDirectionBitMap;

public class SyncableSides extends SyncableObjectBase implements IRpcDirectionBitMap, IBitMap<Direction>, ISyncableValueProvider<Set<Direction>> {

	private final Set<Direction> dirs = EnumSet.noneOf(Direction.class);

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
	public void writeToNBT(CompoundNBT nbt, String name) {
		nbt.setByte(name, (byte)write());
	}

	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		read(nbt.getByte(name));
	}

	@Override
	public Set<Direction> getValue() {
		return Collections.unmodifiableSet(dirs);
	}

	@Override
	public void mark(Direction dir) {
		if (dirs.add(dir)) markDirty();
	}

	@Override
	public void clear(Direction dir) {
		if (dirs.remove(dir)) markDirty();
	}

	@Override
	public boolean get(Direction dir) {
		return dirs.contains(dir);
	}

	@Override
	public void clearAll() {
		dirs.clear();
		markDirty();
	}

	@Override
	public void toggle(Direction value) {
		if (!dirs.remove(value)) dirs.add(value);
		markDirty();
	}

	@Override
	public void set(Direction key, boolean value) {
		if (value) dirs.add(key);
		else dirs.remove(key);
		markDirty();
	}
}
