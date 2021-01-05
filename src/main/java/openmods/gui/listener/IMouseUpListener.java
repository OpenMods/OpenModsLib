package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

@FunctionalInterface
public interface IMouseUpListener extends IListenerBase {
	boolean componentMouseUp(BaseComponent component, int x, int y, int button);
}