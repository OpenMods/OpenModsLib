package openmods.gui.misc;

import java.util.Set;
import net.minecraft.util.EnumFacing;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.utils.bitmap.IWriteableBitMap;

public interface IConfigurableGuiSlots<T extends Enum<T>> {
	IValueProvider<Set<EnumFacing>> createAllowedDirectionsProvider(T slot);

	IWriteableBitMap<EnumFacing> createAllowedDirectionsReceiver(T slot);

	IValueProvider<Boolean> createAutoFlagProvider(T slot);

	IValueReceiver<Boolean> createAutoSlotReceiver(T slot);
}
