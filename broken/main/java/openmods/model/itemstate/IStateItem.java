package openmods.model.itemstate;

import javax.annotation.Nonnull;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.world.World;
import openmods.state.ItemState;

public interface IStateItem {
	ItemState getState(@Nonnull ItemStack stack, World world, LivingEntity entity);
}
