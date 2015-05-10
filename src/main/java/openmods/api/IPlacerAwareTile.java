package openmods.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public interface IPlacerAwareTile {
	public void onBlockPlacedBy(EntityLivingBase placer, ItemStack stack);
}
