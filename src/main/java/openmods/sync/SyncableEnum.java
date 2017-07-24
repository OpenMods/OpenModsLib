package openmods.sync;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class SyncableEnum<E extends Enum<E>> extends SyncableObjectBase implements ISyncableValueProvider<E> {

	public static final Supplier<ISyncableObject> DUMMY_SUPPLIER = new Supplier<ISyncableObject>() {
		@Override
		public ISyncableObject get() {
			return new SyncableObjectBase() {

				@Override
				public void writeToStream(PacketBuffer buf) {
					buf.writeVarInt(0);
				}

				@Override
				public void readFromStream(PacketBuffer buf) {
					buf.readVarInt();
				}

				@Override
				public void writeToNBT(NBTTagCompound nbt, String name) {}

				@Override
				public void readFromNBT(NBTTagCompound nbt, String name) {}
			};
		}
	};

	private final E[] values;

	private E value;

	public static <E extends Enum<E>> SyncableEnum<E> create(E initialValue) {
		return new SyncableEnum<E>(initialValue);
	}

	public SyncableEnum(E value) {
		this.value = value;
		this.values = value.getDeclaringClass().getEnumConstants();
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		int ordinal = stream.readVarInt();
		value = values[ordinal];
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeVarInt(value.ordinal());
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setInteger(name, value.ordinal());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		int original = nbt.getInteger(name);
		value = values[original];
	}

	@Override
	public E getValue() {
		return value;
	}

	public E get() {
		return value;
	}

	public void set(E value) {
		Preconditions.checkNotNull(value);
		if (this.value != value) {
			this.value = value;
			markDirty();
		}
	}

	public E increment() {
		final int next = value.ordinal() + 1;
		if (next >= values.length) value = values[0];
		else value = values[next];
		markDirty();
		return value;
	}

	public E decrement() {
		final int prev = value.ordinal() - 1;
		if (prev < 0) value = values[values.length - 1];
		else value = values[prev];
		markDirty();
		return value;
	}
}
