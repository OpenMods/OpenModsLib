package openmods.calc.types.multi;

import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;

import java.math.BigInteger;

import openmods.calc.Calculator;
import openmods.calc.OperatorDictionary;
import openmods.calc.TopFrame;
import openmods.calc.parsing.StringEscaper;
import openmods.calc.types.bigint.BigIntPrinter;
import openmods.calc.types.fp.DoublePrinter;
import openmods.config.simpler.Configurable;

public class TypedValueCalculator extends Calculator<TypedValue> {

	public static class UnitType {
		public static final UnitType INSTANCE = new UnitType();

		private UnitType() {}

		@Override
		public String toString() {
			return "<null>";
		}
	}

	private static final TCharSet UNESCAPED_CHARS = new TCharHashSet(new char[] { '\'' });

	private final TypeDomain domain;

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
	public boolean printTypes = true;

	private final DoublePrinter doublePrinter = new DoublePrinter(8);

	private final BigIntPrinter bigIntPrinter = new BigIntPrinter();

	public TypedValueCalculator(TypeDomain domain) {
		super(new TypedValueParser(domain), domain.create(UnitType.class, UnitType.INSTANCE));
		this.domain = domain;
	}

	@Override
	protected void setupOperators(OperatorDictionary<TypedValue> operators) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setupGlobals(TopFrame<TypedValue> globals) {
		// TODO Auto-generated method stub

	}

	/*
	 * supported types:
	 * Unit
	 * BigInteger
	 * Double
	 * String
	 * Boolean
	 */

	@Override
	public String toString(TypedValue value) {
		final String contents;
		if (value.type == Double.class) contents = printDouble(value.unwrap(Double.class));
		else if (value.type == BigInteger.class) contents = printBigInteger(value.unwrap(BigInteger.class));
		else if (value.type == String.class) contents = printString(value.unwrap(String.class));
		else if (value.type == Boolean.class) contents = printBoolean(value.unwrap(Boolean.class));
		else contents = value.value.toString();

		return printTypes? "(" + value.type + ")" + contents : contents;
	}

	private String printBoolean(boolean value) {
		return numericBool? (value? "1" : "0") : (value? "True" : "False");
	}

	private String printString(String value) {
		return escapeStrings? StringEscaper.escapeString(value, '"', UNESCAPED_CHARS) : value;
	}

	private String printBigInteger(BigInteger value) {
		if (base < Character.MIN_RADIX) return "invalid radix";
		return decorateBase(!uniformBaseNotation, base, (base <= Character.MAX_RADIX)? value.toString(base) : bigIntPrinter.toString(value, base));
	}

	private String printDouble(Double value) {
		if (base == 10 && !allowStandardPrinter && !uniformBaseNotation) {
			return value.toString();
		} else {
			if (value.isNaN()) return "NaN";
			if (value.isInfinite()) return value > 0? "+Inf" : "-Inf";
			final String result = doublePrinter.toString(value, base);
			return decorateBase(!uniformBaseNotation, base, result);
		}
	}

}
