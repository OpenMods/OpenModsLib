package openmods.serializable;

import net.minecraft.nbt.NBTTagCompound;

public interface INbtSerializable {

	void writeToNBT(NBTTagCompound nbt);

	void readFromNBT(NBTTagCompound nbt);
}
