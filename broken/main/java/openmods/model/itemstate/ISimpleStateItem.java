package openmods.model.itemstate;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import openmods.state.ItemState;

public interface ISimpleStateItem {
	ItemState getState(@Nonnull ItemStack stack);
}
