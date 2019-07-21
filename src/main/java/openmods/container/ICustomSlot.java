package openmods.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;

public interface ICustomSlot {

	ItemStack onClick(PlayerEntity player, int dragType, ClickType clickType);

	boolean canDrag();

	boolean canTransferItemsOut();

	boolean canTransferItemsIn();
}
