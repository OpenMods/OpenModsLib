package openmods.gui.listener;

@FunctionalInterface
public interface IValueChangedListener<T> extends IListenerBase {
	void valueChanged(T value);
}
