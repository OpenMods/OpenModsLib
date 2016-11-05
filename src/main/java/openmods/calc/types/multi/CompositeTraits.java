package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.ICallable;

public class CompositeTraits {

	public interface Truthy extends ICompositeTrait {
		public boolean isTruthy();
	}

	public interface Emptyable extends ICompositeTrait {
		public boolean isEmpty();
	}

	public interface Countable extends ICompositeTrait {
		public int count();
	}

	public interface Structured extends ICompositeTrait {
		public Optional<TypedValue> get(TypeDomain domain, String component);
	}

	public interface Indexable extends ICompositeTrait {
		public Optional<TypedValue> get(TypedValue index);
	}

	public interface Enumerable extends Countable {
		public TypedValue get(TypeDomain domain, int index);
	}

	public interface Callable extends ICompositeTrait, ICallable<TypedValue> {}

}
