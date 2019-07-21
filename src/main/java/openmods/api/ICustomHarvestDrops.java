package openmods.api;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface ICustomHarvestDrops {

	boolean suppressBlockHarvestDrops();

	void addHarvestDrops(PlayerEntity player, List<ItemStack> drops, BlockState blockState, int fortune, boolean isSilkTouch);

}
