package openmods.gui.component;

import java.util.List;

import net.minecraft.client.Minecraft;

import com.google.common.collect.Lists;

public abstract class BaseComposite extends BaseComponent {

	protected final List<BaseComponent> components = Lists.newArrayList();

	public BaseComposite(int x, int y) {
		super(x, y);
	}

	protected boolean areChildrenActive() {
		return true;
	}

	public BaseComposite addComponent(BaseComponent component) {
		components.add(component);
		return this;
	}

	private static boolean isComponentEnabled(BaseComponent component) {
		return component != null && component.isEnabled();
	}

	private static boolean isComponentCapturingMouse(BaseComponent component, int mouseX, int mouseY) {
		return isComponentEnabled(component) && component.isMouseOver(mouseX, mouseY);
	}

	protected abstract void renderComponentBackground(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY);

	protected void renderComponentForeground(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	@Override
	public final void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		renderComponentBackground(minecraft, offsetX, offsetY, mouseX, mouseY);

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentEnabled(component)) {
				component.render(minecraft,
						offsetX + this.x, offsetY + this.y,
						mouseX - this.x, mouseY - this.y);
			}

		renderComponentForeground(minecraft, offsetX, offsetY, mouseX, mouseY);
	}

	protected void renderComponentOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	@Override
	public final void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		renderComponentOverlay(minecraft, offsetX, offsetY, mouseX, mouseY);

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentEnabled(component)) {
				component.renderOverlay(minecraft,
						offsetX + this.x, offsetY + this.y,
						mouseX - this.x, mouseY - this.y);
			}

	}

	@Override
	public void keyTyped(char keyChar, int keyCode) {
		super.keyTyped(keyChar, keyCode);

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentEnabled(component)) {
				component.keyTyped(keyChar, keyCode);
			}
	}

	@Override
	public void mouseDown(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentCapturingMouse(component, mouseX, mouseY)) {
				component.mouseDown(mouseX - component.x, mouseY - component.y, button);
			}
	}

	@Override
	public void mouseUp(int mouseX, int mouseY, int button) {
		super.mouseUp(mouseX, mouseY, button);

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentCapturingMouse(component, mouseX, mouseY)) {
				component.mouseUp(mouseX - component.x, mouseY - component.y, button);
			}
	}

	@Override
	public void mouseDrag(int mouseX, int mouseY, int button, /* love you */long time) {
		super.mouseDrag(mouseX, mouseY, button, time);

		if (!areChildrenActive()) return;

		for (BaseComponent component : components)
			if (isComponentCapturingMouse(component, mouseX, mouseY)) {
				component.mouseDrag(mouseX - component.x, mouseY - component.y, button, time);
			}
	}

}
