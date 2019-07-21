package openmods.serializable;

import net.minecraft.nbt.CompoundNBT;

public interface INbtSerializable {

	void writeToNBT(CompoundNBT nbt);

	void readFromNBT(CompoundNBT nbt);
}
