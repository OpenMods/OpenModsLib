package openmods.gui.component;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import openmods.gui.Icon;
import openmods.gui.misc.BoxRenderer;
import openmods.gui.misc.ISlotBackgroundRenderer;
import openmods.utils.render.RenderUtils;

public class GuiComponentPanel extends GuiComponentResizableComposite {

	private static final BoxRenderer BOX_RENDERER = new BoxRenderer(0, 5);

	public static final ISlotBackgroundRenderer normalSlot = new ISlotBackgroundRenderer() {
		@Override
		public void render(BaseComponent gui, Slot slot) {
			gui.drawTexturedModalRect(slot.xPos - 1, slot.yPos - 1, 0, 20, 18, 18);
		}
	};

	public static final ISlotBackgroundRenderer bigSlot = new ISlotBackgroundRenderer() {
		@Override
		public void render(BaseComponent gui, Slot slot) {
			gui.drawTexturedModalRect(slot.xPos - 5, slot.yPos - 5, 29, 20, 26, 26);
		}
	};

	public static final ISlotBackgroundRenderer noRenderSlot = new ISlotBackgroundRenderer() {
		@Override
		public void render(BaseComponent gui, Slot slot) {}
	};

	public static ISlotBackgroundRenderer coloredSlot(final int color) {
		return new ISlotBackgroundRenderer() {
			@Override
			public void render(BaseComponent gui, Slot slot) {
				RenderUtils.setColor(color);
				gui.drawTexturedModalRect(slot.xPos - 1, slot.yPos - 1, 0, 20, 18, 18);
				GlStateManager.color(1, 1, 1);
			}
		};
	}

	public static ISlotBackgroundRenderer customIconSlot(final Icon icon, final int deltaX, final int deltaY) {
		return new ISlotBackgroundRenderer() {
			@Override
			public void render(BaseComponent gui, Slot slot) {
				gui.drawSprite(icon, slot.xPos + deltaX, slot.yPos + deltaY);

			}
		};
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
	protected void renderComponentBackground(int x, int y, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1);
		bindComponentsSheet();
		BOX_RENDERER.render(this, this.x + x, this.y + y, width, height, 0xFFFFFFFF);
	}

	@Override
	protected void renderComponentForeground(int x, int y, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1);

		if (container != null) {
			for (Slot slot : container.inventorySlots) {
				bindComponentsSheet();
				Objects.firstNonNull(slotRenderers.get(slot.slotNumber), normalSlot).render(this, slot);
			}
		}
	}

}
