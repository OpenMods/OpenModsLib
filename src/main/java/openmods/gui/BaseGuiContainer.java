package openmods.gui;

import openmods.container.ContainerBase;
import openmods.gui.component.BaseComposite;
import openmods.gui.component.GuiComponentPanel;
import openmods.utils.TranslationUtils;

public abstract class BaseGuiContainer<T extends ContainerBase<?>> extends ComponentGui {
	protected final String name;

	public BaseGuiContainer(T container, int width, int height, String name) {
		super(container, width, height);
		this.name = name;
	}

	@Override
	protected BaseComposite createRoot() {
		return new GuiComponentPanel(0, 0, xSize, ySize, getContainer());
	}

	@SuppressWarnings("unchecked")
	public T getContainer() {
		return (T)inventorySlots;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		String machineName = TranslationUtils.translateToLocal(name);
		int x = this.xSize / 2 - (fontRenderer.getStringWidth(machineName) / 2);
		fontRenderer.drawString(machineName, x, 6, 0x404040);
		String translatedName = TranslationUtils.translateToLocal("container.inventory");
		fontRenderer.drawString(translatedName, 8, this.ySize - 96 + 2, 0x404040);
	}

	public void sendButtonClick(int buttonId) {
		this.mc.playerController.sendEnchantPacket(getContainer().windowId, buttonId);
	}

}
