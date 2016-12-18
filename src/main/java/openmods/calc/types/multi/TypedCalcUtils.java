package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolMap;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.calc.parsing.ValueNode;
import openmods.utils.OptionalInt;

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

		throw new IllegalArgumentException("Failed to extract identifier from " + arg);
	}

	public static void expectExactArgCount(OptionalInt actual, int expected) {
		if (!actual.compareIfPresent(expected)) throw new StackValidationException("Expected %s argument(s) but got %s", expected, actual.get());
	}

	public static void expectSingleReturn(OptionalInt returnsCount) {
		if (!returnsCount.compareIfPresent(1)) throw new StackValidationException("Has single result but expected %s", returnsCount.get());
	}

	public static void expectExactReturnCount(OptionalInt expected, int actual) {
		if (!expected.compareIfPresent(actual)) throw new StackValidationException("Has %s result(s) but expected %s", actual, expected.get());
	}

	public static boolean isEqual(Frame<TypedValue> frame, TypedValue left, TypedValue right) {
		if (left.equals(right)) return true;

		{
			final MetaObject.SlotEquals isEquals = left.getMetaObject().slotEquals;
			if (isEquals != null) return isEquals.equals(left, right, frame);
		}

		{
			final MetaObject.SlotEquals isEquals = right.getMetaObject().slotEquals;
			if (isEquals != null) return isEquals.equals(right, left, frame);
		}

		return false;
	}

	public static void matchPattern(IBindPattern pattern, Frame<TypedValue> executionFrame, SymbolMap<TypedValue> outputSymbols, TypedValue value) {
		final boolean matchResult = pattern.match(executionFrame, outputSymbols, value);
		if (!matchResult)
			throw new IllegalArgumentException("Can't match value " + value + " to pattern '" + pattern.serialize() + "'");
	}
}
