package openmods.gui.logic;

import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;

import com.google.common.collect.ImmutableList;

public class ValueCopyAction<T> implements IValueUpdateAction {

	private final IValueProvider<T> provider;
	private final IValueReceiver<T> receiver;

	public ValueCopyAction(IValueProvider<T> provider, IValueReceiver<T> receiver) {
		this.provider = provider;
		this.receiver = receiver;
	}

	@Override
	public Iterable<?> getTriggers() {
		return ImmutableList.of(receiver);
	}

	@Override
	public void execute() {
		T value = provider.getValue();
		receiver.setValue(value);
	}

	public static <T> ValueCopyAction<T> create(IValueProvider<T> provider, IValueReceiver<T> receiver) {
		return new ValueCopyAction<T>(provider, receiver);
	}

}
