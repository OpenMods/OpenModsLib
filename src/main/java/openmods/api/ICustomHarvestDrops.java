package openmods.api;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface ICustomHarvestDrops {

	boolean suppressNormalHarvestDrops();

	void addHarvestDrops(@Nullable EntityPlayer player, List<ItemStack> drops);

}
