package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

public interface IMouseDownListener extends IListenerBase {
	public void componentMouseDown(BaseComponent component, int x, int y, int button);
}