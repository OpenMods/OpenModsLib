package openmods.calc.types.multi;

import com.google.common.base.Optional;
import java.util.List;
import openmods.calc.ICallable;
import openmods.calc.IValuePrinter;

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

	public interface Decomposable extends ICompositeTrait {
		public Optional<List<TypedValue>> tryDecompose(TypedValue input, int variableCount);
	}

	public interface Printable extends ICompositeTrait {
		public String str(IValuePrinter<TypedValue> printer);
	}

	public interface Equatable extends ICompositeTrait {
		public boolean isEqual(TypedValue value);
	}

	public interface TypeMarker extends ICompositeTrait {

	}

	public interface Typed extends ICompositeTrait {
		public TypedValue getType();
	}
}
