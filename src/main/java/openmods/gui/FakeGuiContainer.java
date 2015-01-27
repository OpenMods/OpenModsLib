package openmods.gui;

import java.util.List;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import openmods.container.ContainerBase;
import openmods.container.FakeSlot;
import openmods.events.network.FakeSlotEventPacket;
import openmods.network.DimCoord;
import openmods.tileentity.OpenTileEntity;

public abstract class FakeGuiContainer<T extends ContainerBase<?>> extends BaseGuiContainer {

	public FakeGuiContainer(T container, int width, int height, String name) {
		super(container, width, height, name);
	}

	// The current player clicked a slot on the GUI
	protected void onFakeSlotClick(int index, Slot slot) {
		// This only ever runs on the client side, so we can safely the local
		// thePlayer and his current item.
		ItemStack playerStack = mc.thePlayer.inventory.getItemStack();

		// Create a copy of the itemstack the player is currently holding
		// since we don't want to manipulate it.
		ItemStack newStack = null;
		if (playerStack != null) {
			newStack = playerStack.copy();
		}

		// Wrap everything we need nicely in a NetworkEvent and send it to
		// the server - he will actually change the inventory accordingly.
		Object owner = getContainer().getOwner();
		if (owner instanceof OpenTileEntity) {
			DimCoord dimCoords = ((OpenTileEntity)owner).getDimCoords();
			new FakeSlotEventPacket(newStack, index, dimCoords).sendToServer();
		}
	}

	// Since the Fake Slots do not take items, we need to manually track when
	// a player clicks a slot.
	@Override
	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);

		List<Slot> slots = getContainer().inventorySlots;

		x = x - guiLeft + 1;
		y = y - guiTop + 1;

		int index = 0;
		for (Slot slot : slots) {
			if (slot instanceof FakeSlot) {
				if (x >= slot.xDisplayPosition && x < slot.xDisplayPosition + 18) {
					if (y >= slot.yDisplayPosition && y < slot.yDisplayPosition + 18) {
						onFakeSlotClick(index, slot);
					}
				}
			}

			index++;
		}
	}
}
