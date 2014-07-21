package openmods.gui.listener;

public interface IValueChangedListener<T> extends IListenerBase {
	public void valueChanged(T value);
}
