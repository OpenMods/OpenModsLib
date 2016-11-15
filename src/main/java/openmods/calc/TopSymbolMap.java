package openmods.calc;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.util.Map;

public class TopSymbolMap<E> extends SymbolMap<E> {

	private static class CallableSymbol<E> implements ISymbol<E> {
		private final ICallable<E> callable;

		public CallableSymbol(ICallable<E> callable) {
			this.callable = callable;
		}

		@Override
		public void call(Frame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			callable.call(frame, argumentsCount, returnsCount);
		}

		@Override
		public E get() {
			throw new UnsupportedOperationException("Cannot use function as value");
		}

	}

	private abstract static class ValueSymbol<E> extends SingleReturnCallable<E> implements ISymbol<E> {
		@Override
		public E call(Frame<E> frame, Optional<Integer> argumentsCount) {
			if (argumentsCount.isPresent()) {
				final int args = argumentsCount.get();
				if (args != 0) throw new StackValidationException("Expected no arguments but got %s", args);
			}

			return get();
		}
	}

	private static class GettableSymbol<E> extends ValueSymbol<E> {
		private final IGettable<E> gettable;

		public GettableSymbol(IGettable<E> gettable) {
			this.gettable = gettable;
		}

		@Override
		public E get() {
			return gettable.get();
		}
	}

	private static class ConstantSymbol<E> extends ValueSymbol<E> {
		private final E value;

		public ConstantSymbol(E value) {
			this.value = value;
		}

		@Override
		public E get() {
			return value;
		}
	}

	private final Map<String, ISymbol<E>> globals = Maps.newHashMap();

	@Override
	protected ISymbol<E> createSymbol(ICallable<E> callable) {
		return new CallableSymbol<E>(callable);
	}

	@Override
	protected ISymbol<E> createSymbol(IGettable<E> gettable) {
		return new GettableSymbol<E>(gettable);
	}

	@Override
	protected ISymbol<E> createSymbol(E value) {
		return new ConstantSymbol<E>(value);
	}

	@Override
	public void put(String name, ISymbol<E> symbol) {
		globals.put(name, symbol);
	}

	@Override
	public ISymbol<E> get(String name) {
		return globals.get(name);
	}

}
