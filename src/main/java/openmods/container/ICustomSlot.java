package openmods.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;

public interface ICustomSlot {

	public ItemStack onClick(EntityPlayer player, int dragType, ClickType clickType);

	public boolean canDrag();

	public boolean canTransferItemsOut();

	public boolean canTransferItemsIn();
}
