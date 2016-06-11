package openmods.sync;

import com.google.common.collect.Iterators;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.utils.ByteUtils;
import openmods.utils.DirUtils;
import openmods.utils.bitmap.IBitMap;
import openmods.utils.bitmap.IRpcDirectionBitMap;

public class SyncableSides extends SyncableObjectBase implements IRpcDirectionBitMap, IBitMap<ForgeDirection>, ISyncableValueProvider<Set<ForgeDirection>> {

	private Set<ForgeDirection> dirs = EnumSet.noneOf(ForgeDirection.class);

	private void read(int bits) {
		dirs.clear();
		Iterators.addAll(dirs, DirUtils.bitsToValidDirs(bits));
	}

	private int write() {
		return ByteUtils.enumSetToBits(dirs);
	}

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		read(stream.readByte());
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
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
	public Set<ForgeDirection> getValue() {
		return Collections.unmodifiableSet(dirs);
	}

	@Override
	public void mark(ForgeDirection dir) {
		if (dirs.add(dir)) markDirty();
	}

	@Override
	public void clear(ForgeDirection dir) {
		if (dirs.remove(dir)) markDirty();
	}

	@Override
	public boolean get(ForgeDirection dir) {
		return dirs.contains(dir);
	}

	@Override
	public void clearAll() {
		dirs.clear();
		markDirty();
	}

	@Override
	public void toggle(ForgeDirection value) {
		if (!dirs.remove(value)) dirs.add(value);
		markDirty();
	}

	@Override
	public void set(ForgeDirection key, boolean value) {
		if (value) dirs.add(key);
		else dirs.remove(key);
		markDirty();
	}
}
