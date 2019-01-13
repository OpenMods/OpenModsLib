package openmods.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;

public interface ICustomSlot {

	ItemStack onClick(EntityPlayer player, int dragType, ClickType clickType);

	boolean canDrag();

	boolean canTransferItemsOut();

	boolean canTransferItemsIn();
}
