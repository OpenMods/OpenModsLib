package openmods.gui.listener;

import openmods.gui.component.BaseComponent;

public interface IKeyTypedListener extends IListenerBase {
	public void componentKeyTyped(BaseComponent component, char character, int keyCode);
}