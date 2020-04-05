package openmods.model.itemstate;

import javax.annotation.Nonnull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import openmods.state.ItemState;

public interface ISimpleStateItem {
	StateContainer<Item, ItemState> getStateContainer();

	ItemState getState(@Nonnull ItemStack stack);
}
