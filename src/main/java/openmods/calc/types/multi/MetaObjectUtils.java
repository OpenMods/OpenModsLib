package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.utils.OptionalInt;

public class MetaObjectUtils {

	public static final MetaObject.SlotBool BOOL_ALWAYS_TRUE = new MetaObject.SlotBool() {
		@Override
		public boolean bool(TypedValue self, Frame<TypedValue> frame) {
			return true;
		}
	};
	public static final MetaObject.SlotBool BOOL_ALWAYS_FALSE = new MetaObject.SlotBool() {
		@Override
		public boolean bool(TypedValue self, Frame<TypedValue> frame) {
			return false;
		}
	};
	public static final MetaObject.SlotEquals USE_VALUE_EQUALS = new MetaObject.SlotEquals() {
		@Override
		public boolean equals(TypedValue self, TypedValue other, Frame<TypedValue> frame) {
			return self.equals(other);
		}
	};

	public static MetaObject.SlotStr strConst(final String value) {
		return new MetaObject.SlotStr() {
			@Override
			public String str(TypedValue self, Frame<TypedValue> frame) {
				return value;
			}
		};
	}

	public static MetaObject.SlotRepr reprConst(final String value) {
		return new MetaObject.SlotRepr() {
			@Override
			public String repr(TypedValue self, Frame<TypedValue> frame) {
				return value;
			}
		};
	}

	public static MetaObject.SlotType typeConst(final TypedValue value) {
		return new MetaObject.SlotType() {
			@Override
			public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
				return value;
			}
		};
	}

	public static MetaObject.SlotCall callableAdapter(final ICallable<TypedValue> callable) {
		return new MetaObject.SlotCall() {
			@Override
			public void call(TypedValue self, OptionalInt argCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
				callable.call(frame, argCount, returnsCount);
			}
		};
	}

	public static MetaObject.SlotAttr attrFromMap(final Map<String, TypedValue> attrs) {
		return new MetaObject.SlotAttr() {
			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				return Optional.fromNullable(attrs.get(key));
			}
		};
	}

	public static String callStrSlot(Frame<TypedValue> frame, TypedValue value) {
		final MetaObject.SlotStr slotStr = value.getMetaObject().slotStr;
		return slotStr != null? slotStr.str(value, frame) : value.toString();
	}

	public static String callReprSlot(Frame<TypedValue> frame, TypedValue value) {
		final MetaObject.SlotRepr slotRepr = value.getMetaObject().slotRepr;
		return slotRepr != null? slotRepr.repr(value, frame) : value.toString();
	}

	public static void call(Frame<TypedValue> frame, TypedValue target, OptionalInt argumentsCount, OptionalInt returnsCount) {
		final MetaObject.SlotCall slotCall = target.getMetaObject().slotCall;
		Preconditions.checkState(slotCall != null, "Value %s is not callable", target);
		slotCall.call(target, argumentsCount, returnsCount, frame);

	}

	public static boolean isCallable(TypedValue arg) {
		return arg.getMetaObject().slotCall != null;
	}

	public static boolean boolValue(Frame<TypedValue> frame, TypedValue selfValue) {
		final MetaObject.SlotBool slotBool = selfValue.getMetaObject().slotBool;
		Preconditions.checkState(slotBool != null, "Value %s has no bool value", selfValue);
		return slotBool.bool(selfValue, frame);
	}

	public static final MetaObject.SlotDecompose DECOMPOSE_ON_TYPE = new MetaObject.SlotDecompose() {
		@Override
		public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
			final MetaObject.SlotType valueTypeSlot = input.getMetaObject().slotType;
			if (valueTypeSlot != null) {

				final TypedValue valueType = valueTypeSlot.type(input, frame);
				if (TypedCalcUtils.isEqual(frame, self, valueType)) {
					List<TypedValue> result = ImmutableList.of(input);
					return Optional.of(result);
				}
			}

			return Optional.absent();
		}
	};

}
