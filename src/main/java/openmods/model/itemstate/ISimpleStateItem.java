package openmods.model.itemstate;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import openmods.state.State;
import openmods.state.StateContainer;

public interface ISimpleStateItem {

	public StateContainer getStateContainer();

	public State getState(@Nonnull ItemStack stack);

}
