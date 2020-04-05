package openmods.gui;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import openmods.container.ContainerBase;
import openmods.gui.component.BaseComposite;
import openmods.gui.component.GuiComponentPanel;

public abstract class BaseGuiContainer<T extends ContainerBase<?>> extends ComponentGui<T> {
	public BaseGuiContainer(T container, PlayerInventory inv, ITextComponent name, int width, int height) {
		super(container, inv, name, width, height);
	}

	@Override
	protected BaseComposite createRoot() {
		return new GuiComponentPanel(0, 0, xSize, ySize, getContainer());
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		final String machineName = getTitle().getFormattedText();
		int x = this.xSize / 2 - (font.getStringWidth(machineName) / 2);
		font.drawString(machineName, x, 6, 0x404040);
		String translatedName = playerInventory.getDisplayName().getFormattedText();
		font.drawString(translatedName, 8, this.ySize - 96 + 2, 0x404040);
	}

	public void sendButtonClick(int buttonId) {
		minecraft.playerController.sendEnchantPacket(getContainer().windowId, buttonId);
	}

}
