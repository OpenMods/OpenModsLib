package openmods.api;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface ICustomHarvestDrops {

	boolean suppressBlockHarvestDrops();

	void addHarvestDrops(EntityPlayer player, List<ItemStack> drops, int fortune, boolean isSilkTouch);

}
