package openmods.model.itemstate;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import openmods.state.State;
import openmods.state.StateContainer;

public interface IStateItem {

	public StateContainer getStateContainer();

	public State getState(ItemStack stack, World world, EntityLivingBase entity);

}
