package openmods.gui.component;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import openmods.gui.misc.BoxRenderer;
import openmods.gui.misc.ISlotBackgroundRenderer;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

public class GuiComponentPanel extends GuiComponentResizableComposite {

	private static final BoxRenderer BOX_RENDERER = new BoxRenderer(0, 5);

	public static final ISlotBackgroundRenderer normalSlot = new ISlotBackgroundRenderer() {
		@Override
		public void render(Gui gui, Slot slot) {
			gui.drawTexturedModalRect(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1, 0, 20, 18, 18);
		}
	};

	public static final ISlotBackgroundRenderer bigSlot = new ISlotBackgroundRenderer() {
		@Override
		public void render(Gui gui, Slot slot) {
			gui.drawTexturedModalRect(slot.xDisplayPosition - 5, slot.yDisplayPosition - 5, 29, 20, 26, 26);
		}
	};

	public static final ISlotBackgroundRenderer noRenderSlot = new ISlotBackgroundRenderer() {
		@Override
		public void render(Gui gui, Slot slot) {}
	};

	private final Map<Integer, ISlotBackgroundRenderer> slotRenderers = Maps.newHashMap();

	private WeakReference<Container> container;

	public GuiComponentPanel(int x, int y, int width, int height, Container container) {
		super(x, y, width, height);
		this.container = new WeakReference<Container>(container);
	}

	public void setSlotRenderer(int slotId, ISlotBackgroundRenderer renderer) {
		slotRenderers.put(slotId, renderer);
	}

	@Override
	protected void renderComponentBackground(Minecraft minecraft, int x, int y, int mouseX, int mouseY) {
		GL11.glColor3f(1, 1, 1);
		bindComponentsSheet();
		BOX_RENDERER.render(this, this.x + x, this.y + y, width, height, 0xFFFFFFFF);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void renderComponentForeground(Minecraft minecraft, int x, int y, int mouseX, int mouseY) {
		GL11.glColor3f(1, 1, 1);
		bindComponentsSheet();

		if (container != null && container.get() != null) {
			for (Slot slot : (List<Slot>)container.get().inventorySlots) {
				Objects.firstNonNull(slotRenderers.get(slot.slotNumber), normalSlot).render(this, slot);
			}
		}
	}

}
