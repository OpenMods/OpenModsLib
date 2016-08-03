package openmods.calc.types.multi;

import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import java.math.BigInteger;
import openmods.calc.IValuePrinter;
import openmods.calc.PositionalNotationPrinter;
import openmods.calc.PrinterUtils;
import openmods.calc.parsing.StringEscaper;
import openmods.calc.types.bigint.BigIntPrinter;
import openmods.calc.types.fp.DoublePrinter;
import openmods.calc.types.multi.Cons.Visitor;
import openmods.config.simpler.Configurable;
import openmods.math.Complex;

public class TypedValuePrinter implements IValuePrinter<TypedValue> {

	private static final TCharSet UNESCAPED_CHARS = new TCharHashSet(new char[] { '\'' });

	@Configurable
	public int base = 10;

	@Configurable
	public boolean uniformBaseNotation = false;

	@Configurable
	public boolean allowStandardPrinter = false;

	@Configurable
	public boolean escapeStrings = true;

	@Configurable
	public boolean numericBool = false;

	@Configurable
	public boolean printTypes = false;

	@Configurable
	public boolean printLists = true;

	@Configurable
	public boolean printNilInLists = false;

	private final PositionalNotationPrinter<Double> doublePrinter = new DoublePrinter.Helper(8);

	private final PositionalNotationPrinter<BigInteger> bigIntPrinter = new BigIntPrinter.Helper(0);

	private final TypedValue nullValue;

	public TypedValuePrinter(TypedValue nullValue) {
		this.nullValue = nullValue;
	}

	@Override
	public String toString(TypedValue value) {
		final String contents;
		if (value.type == Double.class) contents = printDouble(value.unwrap(Double.class));
		else if (value.type == BigInteger.class) contents = printBigInteger(value.unwrap(BigInteger.class));
		else if (value.type == String.class) contents = printString(value.unwrap(String.class));
		else if (value.type == Boolean.class) contents = printBoolean(value.unwrap(Boolean.class));
		else if (value.type == Complex.class) contents = printComplex(value.unwrap(Complex.class));
		else if (value.type == IComposite.class) contents = printComposite(value.unwrap(IComposite.class));
		else if (value.type == Cons.class) contents = printCons(value.unwrap(Cons.class));
		else if (value.type == UnitType.class) contents = TypedValueCalculatorFactory.SYMBOL_NULL;
		else contents = value.value.toString();

		return printTypes? "(" + value.type + ")" + contents : contents;
	}

	private String printBoolean(boolean value) {
		return numericBool? (value? "1" : "0") : (value? TypedValueCalculatorFactory.SYMBOL_FALSE : TypedValueCalculatorFactory.SYMBOL_TRUE);
	}

	private String printString(String value) {
		return escapeStrings? StringEscaper.escapeString(value, '"', UNESCAPED_CHARS) : value;
	}

	private String printBigInteger(BigInteger value) {
		if (base < Character.MIN_RADIX) return "invalid radix";
		return PrinterUtils.decorateBase(!uniformBaseNotation, base, (base <= Character.MAX_RADIX)? value.toString(base) : bigIntPrinter.toString(value, base));
	}

	private String printDouble(Double value) {
		if (base == 10 && !allowStandardPrinter && !uniformBaseNotation) {
			return value.toString();
		} else {
			if (value.isNaN()) return "NaN";
			if (value.isInfinite()) return value > 0? "+Inf" : "-Inf";
			final String result = doublePrinter.toString(value, base);
			return PrinterUtils.decorateBase(!uniformBaseNotation, base, result);
		}
	}

	private String printComplex(Complex value) {
		return printDouble(value.re) + "+" + printDouble(value.im) + "I";
	}

	private String printCons(Cons cons) {
		if (printLists) {
			final StringBuilder result = new StringBuilder();
			cons.visit(new Cons.Visitor() {
				@Override
				public void begin() {
					result.append("(");
				}

				@Override
				public void value(TypedValue value, boolean isLast) {
					result.append(TypedValuePrinter.this.toString(value));
					if (!isLast) result.append(" ");
				}

				@Override
				public Visitor nestedValue(TypedValue value) {
					result.append("(");
					return this;
				}

				@Override
				public void end(TypedValue terminator) {
					if (terminator.value != nullValue || printNilInLists) {
						result.append(" . ");
						result.append(TypedValuePrinter.this.toString(terminator));
					}
					result.append(")");
				}
			});

			return result.toString();
		} else {
			return "(" + toString(cons.car) + " . " + toString(cons.cdr) + ")";
		}
	}

	private static String printComposite(IComposite value) {
		return "<" + value.subtype() + ":" + System.identityHashCode(value) + " " + value.toString() + ">";
	}
}
