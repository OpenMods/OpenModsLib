package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

@FunctionalInterface
public interface IKeyTypedListener extends IListenerBase {
	void componentKeyTyped(BaseComponent component, char character, int keyCode);
}