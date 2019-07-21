package openmods.api;

import javax.annotation.Nonnull;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface IPlaceAwareTile {
	void onBlockPlacedBy(BlockState state, LivingEntity placer, @Nonnull ItemStack stack);
}
