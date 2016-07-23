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

	public interface Visitor {
		public void begin();

		public void value(TypedValue value, boolean isLast);

		public Visitor nestedValue(TypedValue value);

		public void end(TypedValue terminator);
	}

	public void visit(Visitor visitor) {
		visitor.begin();

		Cons e = this;
		while (true) {
			final boolean hasNext = e.cdr.is(Cons.class);
			if (e.car.is(Cons.class)) {
				final Visitor nestedVisitor = visitor.nestedValue(car);
				e.car.unwrap(Cons.class).visit(nestedVisitor);
			} else {
				visitor.value(e.car, !hasNext);
			}

			if (hasNext) {
				e = e.cdr.unwrap(Cons.class);
			} else {
				break;
			}
		}

		visitor.end(e.cdr);
	}

	public String prettyPrint() {
		final StringBuilder result = new StringBuilder();
		visit(new Visitor() {
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
			public Visitor nestedValue(TypedValue value) {
				result.append("(");
				return this;
			}

			@Override
			public void end(TypedValue terminator) {
				result.append(" . ");
				result.append(terminator);
			}
		});

		return result.toString();
	}

	public int length() {
		int result = 1;
		TypedValue c = cdr;
		while (c.is(Cons.class)) {
			c = c.unwrap(Cons.class).cdr;
			result++;
		}

		return result;
	}
}
