package openmods.gui.misc;

import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.utils.bitmap.IWriteableBitMap;

public interface IConfigurableGuiSlots<T extends Enum<T>> {
	public IValueProvider<Set<ForgeDirection>> createAllowedDirectionsProvider(T slot);

	public IWriteableBitMap<ForgeDirection> createAllowedDirectionsReceiver(T slot);

	public IValueProvider<Boolean> createAutoFlagProvider(T slot);

	public IValueReceiver<Boolean> createAutoSlotReceiver(T slot);
}
