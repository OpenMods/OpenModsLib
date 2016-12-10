package openmods.calc.types.multi;

import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import java.math.BigInteger;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.IValuePrinter;
import openmods.calc.PositionalNotationPrinter;
import openmods.calc.PrinterUtils;
import openmods.calc.parsing.StringEscaper;
import openmods.calc.types.bigint.BigIntPrinter;
import openmods.calc.types.fp.DoublePrinter;
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
	public boolean escapeStrings = false;

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
	public String str(TypedValue value) {
		final String contents;
		final MetaObject.SlotStr slotStr = value.getMetaObject().slotStr;
		if (slotStr != null) {
			final Frame<TypedValue> frame = FrameFactory.createTopFrame(); // TODO: is this safe? Probably yes
			contents = slotStr.str(value, frame);
		} else {
			contents = value.value.toString();
		}

		return printTypes? "(" + value.type + ")" + contents : contents;
	}

	@Override
	public String repr(TypedValue value) {
		final MetaObject.SlotRepr slotRepr = value.getMetaObject().slotRepr;
		if (slotRepr != null) {
			final Frame<TypedValue> frame = FrameFactory.createTopFrame(); // TODO: is this safe? Probably yes
			return slotRepr.repr(value, frame);
		} else return value.value.toString();
	}

	public String str(boolean value) {
		return numericBool? (value? "1" : "0") : (value? TypedCalcConstants.SYMBOL_FALSE : TypedCalcConstants.SYMBOL_TRUE);
	}

	public String repr(boolean value) {
		return value? TypedCalcConstants.SYMBOL_FALSE : TypedCalcConstants.SYMBOL_TRUE;
	}

	public String str(String value) {
		return escapeStrings? StringEscaper.escapeString(value, '"', UNESCAPED_CHARS) : value;
	}

	public String repr(String value) {
		return StringEscaper.escapeString(value, '"', UNESCAPED_CHARS);
	}

	public String str(BigInteger value) {
		if (base < Character.MIN_RADIX) return "invalid radix";
		return PrinterUtils.decorateBase(!uniformBaseNotation, base, (base <= Character.MAX_RADIX)? value.toString(base) : bigIntPrinter.toString(value, base));
	}

	public String repr(BigInteger value) {
		return str(value);
	}

	public String str(Double value) {
		if (base == 10 && !allowStandardPrinter && !uniformBaseNotation) {
			return value.toString();
		} else {
			if (value.isNaN()) return "NaN";
			if (value.isInfinite()) return value > 0? "+Inf" : "-Inf";
			final String result = doublePrinter.toString(value, base);
			return PrinterUtils.decorateBase(!uniformBaseNotation, base, result);
		}
	}

	public String repr(Double value) {
		return str(value);
	}

	public String str(Complex value) {
		return str(value.re) + "+" + str(value.im) + "I";
	}

	public String repr(Complex value) {
		return str(value);
	}

	public String str(Cons cons) {
		if (printLists) {
			final StringBuilder result = new StringBuilder();
			cons.visit(new Cons.BranchingVisitor() {
				@Override
				public void begin() {
					result.append("[");
				}

				@Override
				public void value(TypedValue value, boolean isLast) {
					result.append(TypedValuePrinter.this.str(value));
					if (!isLast) result.append(" ");
				}

				@Override
				public Cons.BranchingVisitor nestedValue(TypedValue value) {
					result.append("[");
					return this;
				}

				@Override
				public void end(TypedValue terminator) {
					if (terminator.value != nullValue || printNilInLists) {
						result.append(" . ");
						result.append(TypedValuePrinter.this.str(terminator));
					}
					result.append("]");
				}
			});

			return result.toString();
		} else {
			return "(" + str(cons.car) + " . " + str(cons.cdr) + ")";
		}
	}

	public String repr(Cons cons) {
		// TODO: [] notation? problem with terminators
		return repr(cons.car) + " : " + repr(cons.cdr);
	}

	public String str(Symbol s) {
		return s.value;
	}

	public String repr(Symbol s) {
		return '#' + s.value;
	}

}
