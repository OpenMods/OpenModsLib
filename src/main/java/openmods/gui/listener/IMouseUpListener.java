package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

@FunctionalInterface
public interface IMouseUpListener extends IListenerBase {
	void componentMouseUp(BaseComponent component, int x, int y, int button);
}