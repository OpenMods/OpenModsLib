package openmods.gui.component;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import openmods.gui.listener.*;
import openmods.utils.TextureUtils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Lists;

public abstract class BaseComponent extends Gui {

	public abstract static class ListenerNotifier<T> {
		private final Class<? extends T> selectedClass;

		protected ListenerNotifier(Class<? extends T> selectedClass) {
			this.selectedClass = selectedClass;
		}

		protected abstract void call(T listener);

		@SuppressWarnings("unchecked")
		private void notify(Iterable<IListenerBase> listeners) {
			for (IListenerBase listener : listeners)
				if (selectedClass.isInstance(listener)) call((T)listener);
		}
	}

	private static final int CRAZY_1 = 0x505000FF;
	private static final int CRAZY_2 = (CRAZY_1 & 0xFEFEFE) >> 1 | CRAZY_1 & -0xFF000000;
	private static final int CRAZY_3 = 0xF0100010;

	public final static ResourceLocation TEXTURE_SHEET = new ResourceLocation("openmodslib", "textures/gui/components.png");

	protected void bindComponentsSheet() {
		TextureUtils.bindTextureToClient(TEXTURE_SHEET);
	}

	public enum TabColor {
		blue(0x8784c8),
		lightblue(0x84c7c8),
		green(0x84c892),
		yellow(0xc7c884),
		red(0xc88a84),
		purple(0xc884bf);

		private int color;

		TabColor(int col) {
			this.color = col;
		}

		public int getColor() {
			return color;
		}
	}

	protected String name = null;
	protected int x;
	protected int y;
	protected boolean enabled = true;

	public BaseComponent(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public String getName() {
		return name;
	}

	public BaseComponent setName(String name) {
		this.name = name;
		return this;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	protected boolean areChildrenActive() {
		return true;
	}

	/**
	 * If the mouse position is inside this component
	 * 
	 * @param mouseX
	 *            X position relative from this components parent
	 * @param mouseY
	 *            Y position relative from this components parent
	 * @return true if the X and Y are inside this components area
	 */
	public boolean isMouseOver(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}

	private List<IListenerBase> listeners = Lists.newArrayList();
	public List<BaseComponent> components = new ArrayList<BaseComponent>();

	public BaseComponent addComponent(BaseComponent component) {
		components.add(component);
		return this;
	}

	public BaseComponent childByName(String componentName) {
		if (componentName == null) return null;
		for (BaseComponent component : components) {
			if (componentName.equals(component.getName())) { return component; }
		}
		return null;
	}

	public void addListener(IListenerBase listener) {
		listeners.add(listener);
	}

	public void removeListener(IListenerBase listener) {
		if (!listeners.contains(listener)) return;
		listeners.remove(listener);
	}

	public void clearListeners() {
		listeners.clear();
	}

	private static boolean isComponentEnabled(BaseComponent component) {
		return component != null && component.isEnabled();
	}

	private static boolean isComponentCapturingMouse(BaseComponent component, int mouseX, int mouseY) {
		return isComponentEnabled(component) && component.isMouseOver(mouseX, mouseY);
	}

	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentEnabled(component)) {
				component.render(minecraft,
						offsetX + this.x, offsetY + this.y,
						mouseX - this.x, mouseY - this.y);
			}
	}

	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentEnabled(component)) {
				component.renderOverlay(minecraft,
						offsetX + this.x, offsetY + this.y,
						mouseX - this.x, mouseY - this.y);
			}

	}

	public void keyTyped(char keyChar, int keyCode) {
		for (IListenerBase listener : listeners)
			if (listener instanceof IKeyTypedListener) {
				((IKeyTypedListener)listener).componentKeyTyped(this, keyChar, keyCode);
			}

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentEnabled(component)) {
				component.keyTyped(keyChar, keyCode);
			}
	}

	public void mouseDown(int mouseX, int mouseY, int button) {
		for (IListenerBase listener : listeners)
			if (listener instanceof IMouseDownListener) {
				((IMouseDownListener)listener).componentMouseDown(this, mouseX, mouseY, button);
			}

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentCapturingMouse(component, mouseX, mouseY)) {
				component.mouseDown(mouseX - component.x, mouseY - component.y, button);
			}
	}

	public void mouseUp(int mouseX, int mouseY, int button) {
		for (IListenerBase listener : listeners)
			if (listener instanceof IMouseMoveListener) {
				((IMouseUpListener)listener).componentMouseUp(this, mouseX, mouseY, button);
			}

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentCapturingMouse(component, mouseX, mouseY)) {
				component.mouseUp(mouseX - component.x, mouseY - component.y, button);
			}
	}

	public void mouseDrag(int mouseX, int mouseY, int button, /* love you */long time) {
		for (IListenerBase listener : listeners)
			if (listener instanceof IMouseDragListener) {
				((IMouseDragListener)listener).componentMouseDrag(this, mouseX, mouseY, button, time);
			}

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentCapturingMouse(component, mouseX, mouseY)) {
				component.mouseDrag(mouseX - component.x, mouseY - component.y, button, time);
			}
	}

	protected void notifyListeners(ListenerNotifier<?> selector) {
		selector.notify(listeners);
	}

	protected void drawHoveringText(List<String> lines, int x, int y, FontRenderer font) {
		final int lineCount = lines.size();
		if (lineCount == 0) return;

		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		int width = 0;

		for (String s : lines) {
			int l = font.getStringWidth(s);
			if (l > width) width = l;
		}

		final int left = x + 12;
		int top = y - 12;

		int height = 8;
		if (lineCount > 1) height += 2 + (lineCount - 1) * 10;

		this.zLevel = 350.0F;

		drawGradientRect(left - 3, top - 4, left + width + 3, top - 3, CRAZY_3, CRAZY_3);
		drawGradientRect(left - 3, top + height + 3, left + width + 3, top + height + 4, CRAZY_3, CRAZY_3);
		drawGradientRect(left - 3, top - 3, left + width + 3, top + height + 3, CRAZY_3, CRAZY_3);
		drawGradientRect(left - 4, top - 3, left - 3, top + height + 3, CRAZY_3, CRAZY_3);
		drawGradientRect(left + width + 3, top - 3, left + width + 4, top + height + 3, CRAZY_3, CRAZY_3);

		drawGradientRect(left - 3, top - 3 + 1, left - 3 + 1, top + height + 3 - 1, CRAZY_1, CRAZY_2);
		drawGradientRect(left + width + 2, top - 3 + 1, left + width + 3, top + height + 3 - 1, CRAZY_1, CRAZY_2);
		drawGradientRect(left - 3, top - 3, left + width + 3, top - 3 + 1, CRAZY_1, CRAZY_1);
		drawGradientRect(left - 3, top + height + 2, left + width + 3, top + height + 3, CRAZY_2, CRAZY_2);

		for (int i = 0; i < lineCount; ++i) {
			String line = lines.get(i);
			font.drawStringWithShadow(line, left, top, -1);
			if (i == 0) top += 2;
			top += 10;
		}

		this.zLevel = 0.0F;
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
	}
}
