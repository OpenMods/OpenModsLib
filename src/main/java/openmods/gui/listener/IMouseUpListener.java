package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

public interface IMouseUpListener extends IListenerBase {
	public void componentMouseUp(BaseComponent component, int x, int y, int button);
}