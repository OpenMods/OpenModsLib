package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.IValuePrinter;

public class TypeComposite extends SimpleComposite implements CompositeTraits.Decomposable, CompositeTraits.Printable, CompositeTraits.Structured {

	private final Class<?> tag;
	private final String type;

	private TypeComposite(TypeDomain domain, Class<?> tag) {
		domain.checkIsKnownType(tag);
		this.tag = tag;
		this.type = domain.getName(tag);
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public Optional<List<TypedValue>> tryDecompose(TypedValue input, int variableCount) {
		if (!input.is(tag)) return Optional.absent();

		final List<TypedValue> result = ImmutableList.of(input);
		return Optional.of(result);
	}

	@Override
	public String str(IValuePrinter<TypedValue> printer) {
		return "type: '" + type + "'";
	}

	@Override
	public Optional<TypedValue> get(TypeDomain domain, String component) {
		if (component.equals("name")) return Optional.of(domain.create(String.class, type));
		return Optional.absent();
	}

	private static class Callable extends TypeComposite implements CompositeTraits.Callable {

		private final ICallable<TypedValue> callable;

		private Callable(TypeDomain domain, Class<?> tag, ICallable<TypedValue> callable) {
			super(domain, tag);
			this.callable = callable;
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			callable.call(frame, argumentsCount, returnsCount);
		}

	}

	public static TypedValue create(TypeDomain domain, Class<?> tag) {
		return domain.create(IComposite.class, new TypeComposite(domain, tag));
	}

	public static TypedValue create(TypeDomain domain, Class<?> tag, ICallable<TypedValue> callable) {
		return domain.create(IComposite.class, new Callable(domain, tag, callable));
	}

}
