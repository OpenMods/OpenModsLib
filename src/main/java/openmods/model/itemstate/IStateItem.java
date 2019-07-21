package openmods.model.itemstate;

import javax.annotation.Nonnull;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import openmods.state.State;
import openmods.state.StateContainer;

public interface IStateItem {

	StateContainer getStateContainer();

	State getState(@Nonnull ItemStack stack, World world, LivingEntity entity);

}
