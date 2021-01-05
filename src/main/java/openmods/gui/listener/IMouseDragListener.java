package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

@FunctionalInterface
public interface IMouseDragListener extends IListenerBase {
	boolean componentMouseDrag(BaseComponent component, int x, int y, int button, int dx, int dy);
}