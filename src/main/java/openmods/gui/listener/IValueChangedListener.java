package openmods.gui.listener;

@FunctionalInterface
public interface IValueChangedListener<T> extends IListenerBase {
	public void valueChanged(T value);
}
