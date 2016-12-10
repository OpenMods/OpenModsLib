package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.Frame;

public class MetaObject {

	private MetaObject(Builder builder) {
		this.slotAttr = builder.slotAttr;
		this.slotBool = builder.slotBool;
		this.slotCall = builder.slotCall;
		this.slotEquals = builder.slotEquals;
		this.slotLength = builder.slotLength;
		this.slotRepr = builder.slotRepr;
		this.slotSlice = builder.slotSlice;
		this.slotStr = builder.slotStr;
		this.slotType = builder.slotType;
		this.slotDecompose = builder.slotDecompose;
	}

	public interface SlotBool {
		public boolean bool(TypedValue self, Frame<TypedValue> frame);
	}

	public final SlotBool slotBool;

	public interface SlotLength {
		public int length(TypedValue self, Frame<TypedValue> frame);
	}

	public final SlotLength slotLength;

	public interface SlotAttr {
		public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame);
	}

	public final SlotAttr slotAttr;

	public interface SlotEquals {
		public boolean equals(TypedValue self, TypedValue value, Frame<TypedValue> frame);
	}

	public final SlotEquals slotEquals;

	public interface SlotCall {
		public void call(TypedValue self, Optional<Integer> argumentsCount, Optional<Integer> returnsCount, Frame<TypedValue> frame);
	}

	public final SlotCall slotCall;

	public interface SlotType {
		public TypedValue type(TypedValue self, Frame<TypedValue> frame);
	}

	public final SlotType slotType;

	public interface SlotSlice {
		public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame);
	}

	public final SlotSlice slotSlice;

	public interface SlotStr {
		public String str(TypedValue self, Frame<TypedValue> frame);
	}

	public final SlotStr slotStr;

	public interface SlotRepr {
		public String repr(TypedValue self, Frame<TypedValue> frame);
	}

	public final SlotRepr slotRepr;

	public interface SlotDecompose {
		public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame);
	}

	public final SlotDecompose slotDecompose;

	public static class Builder {
		private SlotBool slotBool;

		private SlotLength slotLength;

		private SlotAttr slotAttr;

		private SlotEquals slotEquals;

		private SlotCall slotCall;

		private SlotType slotType;

		private SlotSlice slotSlice;

		private SlotStr slotStr;

		private SlotRepr slotRepr;

		private SlotDecompose slotDecompose;

		public Builder set(SlotBool slotBool) {
			Preconditions.checkState(this.slotBool == null);
			this.slotBool = slotBool;
			return this;
		}

		public Builder set(SlotLength slotLength) {
			Preconditions.checkState(this.slotLength == null);
			this.slotLength = slotLength;
			return this;
		}

		public Builder set(SlotAttr slotAttr) {
			Preconditions.checkState(this.slotAttr == null);
			this.slotAttr = slotAttr;
			return this;
		}

		public Builder set(SlotEquals slotEquals) {
			Preconditions.checkState(this.slotEquals == null);
			this.slotEquals = slotEquals;
			return this;
		}

		public Builder set(SlotCall slotCall) {
			Preconditions.checkState(this.slotCall == null);
			this.slotCall = slotCall;
			return this;
		}

		public Builder set(SlotType slotType) {
			Preconditions.checkState(this.slotType == null);
			this.slotType = slotType;
			return this;
		}

		public Builder set(SlotSlice slotSlice) {
			Preconditions.checkState(this.slotSlice == null);
			this.slotSlice = slotSlice;
			return this;
		}

		public Builder set(SlotStr slotStr) {
			Preconditions.checkState(this.slotStr == null);
			this.slotStr = slotStr;
			return this;
		}

		public Builder set(SlotRepr slotRepr) {
			Preconditions.checkState(this.slotRepr == null);
			this.slotRepr = slotRepr;
			return this;
		}

		public Builder set(SlotDecompose slotDecompose) {
			Preconditions.checkState(this.slotDecompose == null);
			this.slotDecompose = slotDecompose;
			return this;
		}

		public MetaObject build() {
			return new MetaObject(this);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

}
