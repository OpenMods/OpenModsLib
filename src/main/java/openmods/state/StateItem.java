package openmods.state;

import net.minecraft.item.Item;
import net.minecraft.state.StateContainer;

public abstract class StateItem extends Item {
	public StateItem(Properties properties) {
		super(properties);
	}

	public abstract StateContainer<StateItem, ItemState> getStateContainer();

	public abstract ItemState getDefaultState();
}
