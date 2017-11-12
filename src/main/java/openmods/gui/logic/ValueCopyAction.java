package openmods.gui.logic;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;

public class ValueCopyAction<I, O> implements IValueUpdateAction {

	private final Object trigger;
	private final IValueProvider<I> provider;
	private final IValueReceiver<O> receiver;
	private final Function<I, O> converter;

	public ValueCopyAction(Object trigger, IValueProvider<I> provider, IValueReceiver<O> receiver, Function<I, O> converter) {
		this.trigger = trigger;
		this.provider = provider;
		this.receiver = receiver;
		this.converter = converter;
	}

	@Override
	public Iterable<?> getTriggers() {
		return ImmutableList.of(trigger);
	}

	@Override
	public void execute() {
		I input = provider.getValue();
		O output = converter.apply(input);
		receiver.setValue(output);
	}

	public static <T> ValueCopyAction<T, T> create(IValueProvider<T> provider, IValueReceiver<T> receiver) {
		return new ValueCopyAction<>(provider, provider, receiver, Functions.<T> identity());
	}

	public static <T> ValueCopyAction<T, T> create(Object trigger, IValueProvider<T> provider, IValueReceiver<T> receiver) {
		return new ValueCopyAction<>(trigger, provider, receiver, Functions.<T> identity());
	}

	public static <I, O> ValueCopyAction<I, O> create(IValueProvider<I> provider, IValueReceiver<O> receiver, Function<I, O> converter) {
		return new ValueCopyAction<>(provider, provider, receiver, converter);
	}

	public static <I, O> ValueCopyAction<I, O> create(Object trigger, IValueProvider<I> provider, IValueReceiver<O> receiver, Function<I, O> converter) {
		return new ValueCopyAction<>(trigger, provider, receiver, converter);
	}
}
