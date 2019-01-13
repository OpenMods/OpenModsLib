package openmods.api;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface ICustomPickItem {
	@Nonnull ItemStack getPickBlock(EntityPlayer player);
}
