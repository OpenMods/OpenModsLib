package openmods.calc.types.multi;

import openmods.calc.Frame;

class TypeUserdata {
	final String name;

	public TypeUserdata(String name) {
		this.name = name;
	}

	public static final MetaObject.SlotStr typeStrSlot = new MetaObject.SlotStr() {
		@Override
		public String str(TypedValue self, Frame<TypedValue> frame) {
			return "<type " + self.as(TypeUserdata.class).name + ">";
		}
	};

	public static final MetaObject.SlotRepr typeReprSlot = new MetaObject.SlotRepr() {
		@Override
		public String repr(TypedValue self, Frame<TypedValue> frame) {
			return self.as(TypeUserdata.class).name;
		}
	};

	@Override
	public String toString() {
		return "<type: " + name + ">";
	}
}