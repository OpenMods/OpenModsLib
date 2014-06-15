package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

public interface IMouseMoveListener extends IListenerBase {
	public void componentMouseMove(BaseComponent component, int x, int y);
}