package openmods.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.IReorderingProcessor;
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
	protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(matrixStack, mouseX, mouseY);

		IReorderingProcessor processedTitle = getTitle().func_241878_f();
		font.func_238422_b_(matrixStack, processedTitle, (float)(this.xSize / 2 - font.func_243245_a(processedTitle) / 2), (float)6, 0x404040);
		this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(), 8, this.ySize - 96 + 2, 0x404040);
	}

	public void sendButtonClick(int buttonId) {
		minecraft.playerController.sendEnchantPacket(getContainer().windowId, buttonId);
	}

}
