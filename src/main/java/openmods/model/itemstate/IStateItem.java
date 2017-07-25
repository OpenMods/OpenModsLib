package openmods.model.itemstate;

import javax.annotation.Nonnull;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import openmods.state.State;
import openmods.state.StateContainer;

public interface IStateItem {

	public StateContainer getStateContainer();

	public State getState(@Nonnull ItemStack stack, World world, EntityLivingBase entity);

}
