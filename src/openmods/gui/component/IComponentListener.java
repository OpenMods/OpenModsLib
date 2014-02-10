package openmods.gui.component;

public interface IComponentListener {
	void componentMouseDown(BaseComponent component, int offsetX, int offsetY, int button);

	void componentMouseDrag(BaseComponent component, int offsetX, int offsetY, int button, long time);

	void componentMouseMove(BaseComponent component, int offsetX, int offsetY);

	void componentMouseUp(BaseComponent component, int offsetX, int offsetY, int button);

	void componentKeyTyped(BaseComponent component, char par1, int par2);
}