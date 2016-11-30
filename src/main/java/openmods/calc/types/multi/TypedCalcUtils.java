package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.ICallable;
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

	public static <T extends ICompositeTrait> Optional<T> tryGetTrait(TypedValue value, Class<T> trait) {
		if (value.is(IComposite.class)) {
			final IComposite c = value.as(IComposite.class);
			return c.getOptional(trait);
		}

		return Optional.absent();
	}

	public static boolean isCallable(TypedValue value) {
		if (value.is(ICallable.class))
			return true;

		if (value.is(IComposite.class) && value.as(IComposite.class).has(CompositeTraits.Callable.class))
			return true;

		return false;
	}

	public static boolean tryCall(Frame<TypedValue> frame, TypedValue target, Optional<Integer> returns, Optional<Integer> args) {
		if (target.is(ICallable.class)) {
			@SuppressWarnings("unchecked")
			final ICallable<TypedValue> targetCallable = (ICallable<TypedValue>)target.value;
			targetCallable.call(frame, args, returns);
			return true;
		} else if (target.is(IComposite.class)) {
			final IComposite composite = target.as(IComposite.class);
			composite.get(CompositeTraits.Callable.class).call(frame, args, returns);
			return true;
		}

		return false;
	}
}
