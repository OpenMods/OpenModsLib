package openmods.gui.component;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import openmods.gui.misc.ISlotBackgroundRenderer;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

public class GuiComponentPanel extends GuiComponentBox {

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
		super(x, y, width, height, 0, 5, 0xFFFFFF);
		this.container = new WeakReference<Container>(container);
	}

	public void setSlotRenderer(int slotId, ISlotBackgroundRenderer renderer) {
		slotRenderers.put(slotId, renderer);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doRender(Minecraft minecraft, int x, int y, int mouseX, int mouseY) {
		super.doRender(minecraft, x, y, mouseX, mouseY);
		GL11.glColor4f(1, 1, 1, 1);
		bindComponentsSheet();
		if (container != null && container.get() != null) {
			for (Slot slot : (List<Slot>)container.get().inventorySlots) {
				Objects.firstNonNull(slotRenderers.get(slot.slotNumber), normalSlot).render(this, slot);
			}
		}
	}

}
