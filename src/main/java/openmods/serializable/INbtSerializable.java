package openmods.serializable;

import net.minecraft.nbt.NBTTagCompound;

public interface INbtSerializable {

	public void writeToNBT(NBTTagCompound nbt);

	public void readFromNBT(NBTTagCompound nbt);
}
