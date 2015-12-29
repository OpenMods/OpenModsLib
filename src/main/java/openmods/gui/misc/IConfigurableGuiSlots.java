package openmods.gui.misc;

import java.util.Set;

import net.minecraft.util.EnumFacing;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.utils.bitmap.IWriteableBitMap;

public interface IConfigurableGuiSlots<T extends Enum<T>> {
	public IValueProvider<Set<EnumFacing>> createAllowedDirectionsProvider(T slot);

	public IWriteableBitMap<EnumFacing> createAllowedDirectionsReceiver(T slot);

	public IValueProvider<Boolean> createAutoFlagProvider(T slot);

	public IValueReceiver<Boolean> createAutoSlotReceiver(T slot);
}
