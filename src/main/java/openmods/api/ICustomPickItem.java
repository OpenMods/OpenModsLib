package openmods.api;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface ICustomPickItem {
	@Nonnull ItemStack getPickBlock(PlayerEntity player);
}
