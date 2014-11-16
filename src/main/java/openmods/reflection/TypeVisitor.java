package openmods.reflection;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;

public abstract class TypeVisitor<T> {
	private final Class<?> selectedClass;

	protected TypeVisitor(Class<? extends T> selectedClass) {
		this.selectedClass = selectedClass;
	}

	protected TypeVisitor() {
		Type superclass = getClass().getGenericSuperclass();
		checkArgument(superclass instanceof ParameterizedType, "%s isn't parameterized", superclass);
		Type firstArg = ((ParameterizedType)superclass).getActualTypeArguments()[0];
		this.selectedClass = TypeToken.of(firstArg).getRawType();
	}

	protected abstract void visit(T value);

	@SuppressWarnings("unchecked")
	public void visit(Iterable<? super T> values) {
		for (Object listener : values)
			if (selectedClass.isInstance(listener)) visit((T)listener);
	}
}
