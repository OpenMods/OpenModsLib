package openmods.serializable;

import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.io.INBTSerializer;
import openmods.utils.io.INbtReader;
import openmods.utils.io.INbtWriter;

public class NbtSerializable<T> implements INbtSerializable {

	public T value;

	private final String name;

	private final INbtWriter<T> nbtWriter;

	private final INbtReader<T> nbtReader;

	public NbtSerializable(T value, String name, INbtWriter<T> nbtWriter, INbtReader<T> nbtReader) {
		this.value = value;
		this.name = name;
		this.nbtWriter = nbtWriter;
		this.nbtReader = nbtReader;
	}

	public NbtSerializable(T value, String name, INBTSerializer<T> nbtSerializer) {
		this.value = value;
		this.name = name;
		this.nbtWriter = nbtSerializer;
		this.nbtReader = nbtSerializer;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbtWriter.writeToNBT(value, nbt, name);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		value = nbtReader.readFromNBT(nbt, name);
	}

}
