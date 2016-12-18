package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.List;

public class Cons {

	public final TypedValue car;

	public final TypedValue cdr;

	public Cons(TypedValue car, TypedValue cdr) {
		Preconditions.checkNotNull(car);
		Preconditions.checkNotNull(cdr);
		Preconditions.checkArgument(car.domain == cdr.domain, "Mismatched domain on %s %s", car, cdr);
		this.car = car;
		this.cdr = cdr;
	}

	public static TypedValue create(TypeDomain domain, TypedValue car, TypedValue cdr) {
		return domain.create(Cons.class, new Cons(car, cdr));
	}

	public TypedValue first() {
		return car;
	}

	public TypedValue second() {
		return cdr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((car == null)? 0 : car.hashCode());
		result = prime * result + ((cdr == null)? 0 : cdr.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof Cons) {
			final Cons other = (Cons)obj;
			return this.car.equals(other.car) &&
					this.cdr.equals(other.cdr);
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + car + " . " + cdr + ")";
	}

	public static TypedValue createList(List<TypedValue> elements, TypedValue terminatorValue) {
		final int lastElement = elements.size() - 1;
		final TypeDomain domain = terminatorValue.domain;

		TypedValue result = terminatorValue;

		for (int i = lastElement; i >= 0; i--)
			result = domain.create(Cons.class, new Cons(elements.get(i), result));

		return result;
	}

	public interface BranchingVisitor {
		public void begin();

		public void value(TypedValue value, boolean isLast);

		public BranchingVisitor nestedValue(TypedValue value);

		public void end(TypedValue terminator);
	}

	public interface LinearVisitor {
		public void begin();

		public void value(TypedValue value, boolean isLast);

		public void end(TypedValue terminator);
	}

	public abstract static class ListVisitor implements LinearVisitor {

		private final TypedValue expectedTerminator;

		public ListVisitor(TypedValue expectedTerminator) {
			this.expectedTerminator = expectedTerminator;
		}

		@Override
		public void begin() {}

		@Override
		public void end(TypedValue actualTerminator) {
			Preconditions.checkState(actualTerminator.equals(expectedTerminator), "Not valid list: expected %s, got %s", expectedTerminator, actualTerminator);
		}
	}

	public void visit(BranchingVisitor visitor) {
		visitor.begin();

		Cons e = this;
		while (true) {
			final boolean hasNext = e.cdr.is(Cons.class);
			if (e.car.is(Cons.class)) {
				BranchingVisitor nestedVisitor = visitor.nestedValue(car);
				e.car.as(Cons.class).visit(nestedVisitor);
			} else {
				visitor.value(e.car, !hasNext);
			}

			if (!hasNext) break;
			e = e.cdr.as(Cons.class);
		}

		visitor.end(e.cdr);
	}

	public void visit(LinearVisitor visitor) {
		visitor.begin();

		Cons e = this;
		while (true) {
			final boolean hasNext = e.cdr.is(Cons.class);
			visitor.value(e.car, !hasNext);

			if (!hasNext) break;
			e = e.cdr.as(Cons.class);
		}

		visitor.end(e.cdr);
	}

	public String prettyPrint() {
		final StringBuilder result = new StringBuilder();
		visit(new BranchingVisitor() {
			@Override
			public void begin() {
				result.append("(");
			}

			@Override
			public void value(TypedValue value, boolean isLast) {
				result.append(value);
				if (!isLast) result.append(" ");
			}

			@Override
			public BranchingVisitor nestedValue(TypedValue value) {
				result.append("(");
				return this;
			}

			@Override
			public void end(TypedValue terminator) {
				result.append(" . ");
				result.append(terminator);
				result.append(")");
			}
		});

		return result.toString();
	}

	public int length() {
		int result = 1;
		TypedValue c = cdr;
		while (c.is(Cons.class)) {
			c = c.as(Cons.class).cdr;
			result++;
		}

		return result;
	}

	public abstract static class RecursiveVisitor {
		private final TypedValue nullValue;

		public RecursiveVisitor(TypedValue nullValue) {
			this.nullValue = nullValue;
		}

		protected abstract TypedValue processValue(TypedValue head, TypedValue tail);

		protected TypedValue processEnd() {
			return nullValue;
		}

		public TypedValue process(TypedValue list) {
			if (list == nullValue) return processEnd();
			final Cons cons = list.as(Cons.class);
			return processValue(cons.car, cons.cdr);
		}
	}

	public abstract static class TypedRecursiveVisitor<T> {
		private final TypedValue nullValue;

		public TypedRecursiveVisitor(TypedValue nullValue) {
			this.nullValue = nullValue;
		}

		protected abstract T processValue(TypedValue head, TypedValue tail);

		protected abstract T processEnd();

		public T process(TypedValue list) {
			if (list == nullValue) return processEnd();
			final Cons cons = list.as(Cons.class);
			return processValue(cons.car, cons.cdr);
		}
	}
}
