package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

public interface IMouseDragListener extends IListenerBase {
	public void componentMouseDrag(BaseComponent component, int x, int y, int button, long time);
}