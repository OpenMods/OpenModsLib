package openmods.gui.component;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.Map;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import openmods.gui.Icon;
import openmods.gui.misc.BoxRenderer;
import openmods.gui.misc.ISlotBackgroundRenderer;
import openmods.utils.render.RenderUtils;

public class GuiComponentPanel extends GuiComponentResizableComposite {

	private static final BoxRenderer BOX_RENDERER = new BoxRenderer(0, 5);

	public static final ISlotBackgroundRenderer normalSlot = (gui, matrixStack, slot) -> gui.blit(matrixStack, slot.xPos - 1, slot.yPos - 1, 0, 20, 18, 18);

	public static final ISlotBackgroundRenderer bigSlot = (gui, matrixStack, slot) -> gui.blit(matrixStack, slot.xPos - 5, slot.yPos - 5, 29, 20, 26, 26);

	public static final ISlotBackgroundRenderer noRenderSlot = (gui, matrixStack, slot) -> {};

	public static ISlotBackgroundRenderer coloredSlot(final int color) {
		return (gui, matrixStack, slot) -> {
			RenderUtils.setColor(color);
			gui.blit(matrixStack, slot.xPos - 1, slot.yPos - 1, 0, 20, 18, 18);
			GlStateManager.color4f(1, 1, 1, 1);
		};
	}

	public static ISlotBackgroundRenderer customIconSlot(final Icon icon, final int deltaX, final int deltaY) {
		return (gui, matrixStack, slot) -> gui.drawSprite(icon, matrixStack, slot.xPos + deltaX, slot.yPos + deltaY);
	}

	private final Map<Integer, ISlotBackgroundRenderer> slotRenderers = Maps.newHashMap();

	private final Container container;

	public GuiComponentPanel(int x, int y, int width, int height, Container container) {
		super(x, y, width, height);
		this.container = container;
	}

	public void setSlotRenderer(int slotId, ISlotBackgroundRenderer renderer) {
		slotRenderers.put(slotId, renderer);
	}

	@Override
	protected void renderComponentBackground(MatrixStack matrixStack, int x, int y, int mouseX, int mouseY) {
		GlStateManager.color4f(1, 1, 1, 1);
		bindComponentsSheet();
		BOX_RENDERER.render(this, matrixStack, this.x + x, this.y + y, width, height, 0xFFFFFFFF);
	}

	@Override
	protected void renderComponentForeground(MatrixStack matrixStack, int x, int y, int mouseX, int mouseY) {
		GlStateManager.color4f(1, 1, 1, 1);

		if (container != null) {
			for (Slot slot : container.inventorySlots) {
				bindComponentsSheet();
				MoreObjects.firstNonNull(slotRenderers.get(slot.slotNumber), normalSlot).render(this, matrixStack, slot);
			}
		}
	}

}
