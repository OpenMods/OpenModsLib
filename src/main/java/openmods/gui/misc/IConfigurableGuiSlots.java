package openmods.gui.misc;

import java.util.Set;
import net.minecraft.util.Direction;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.utils.bitmap.IWriteableBitMap;

public interface IConfigurableGuiSlots<T extends Enum<T>> {
	IValueProvider<Set<Direction>> createAllowedDirectionsProvider(T slot);

	IWriteableBitMap<Direction> createAllowedDirectionsReceiver(T slot);

	IValueProvider<Boolean> createAutoFlagProvider(T slot);

	IValueReceiver<Boolean> createAutoSlotReceiver(T slot);
}
