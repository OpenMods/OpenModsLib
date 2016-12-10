package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.StackValidationException;
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

		throw new IllegalArgumentException("Failed to extract identifier from " + arg);
	}

	public static void expectSingleReturn(Optional<Integer> returnsCount) {
		if (returnsCount.isPresent()) {
			final int returns = returnsCount.get();
			if (returns != 1) throw new StackValidationException("Has single result but expected %s", returns);
		}
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
}
