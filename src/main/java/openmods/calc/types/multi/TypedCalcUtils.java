package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.calc.parsing.ValueNode;

public class TypedCalcUtils {

	public static TypedValue extractNameFromNode(TypeDomain domain, IExprNode<TypedValue> arg) {
		if (arg instanceof SymbolGetNode)
			return Symbol.get(domain, ((SymbolGetNode<TypedValue>)arg).symbol());

		if (arg instanceof ValueNode) {
			final TypedValue value = ((ValueNode<TypedValue>)arg).value;
			if (value.is(Symbol.class))
				return value;
			if (value.is(String.class))
				return Symbol.get(domain, value.as(String.class));
		}

		throw new IllegalArgumentException();
	}

	public static <T extends ICompositeTrait, R> Optional<R> tryDecomposeTrait(TypedValue value, Class<T> trait, Function<T, Optional<R>> f) {
		if (value.is(IComposite.class)) {
			final IComposite c = value.as(IComposite.class);

			final Optional<T> typeTrait = c.getOptional(trait);
			if (typeTrait.isPresent()) {
				final T container = typeTrait.get();
				return f.apply(container);
			}
		}

		return Optional.absent();
	}
}
