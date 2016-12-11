package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.types.multi.MetaObject.SlotAttr;

class TypeUserdata {
	public static final String ATTR_TYPE_NAME = "name";

	public final String name;

	public TypeUserdata(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "<type: " + name + ">";
	}

	public static final MetaObject.SlotStr defaultStrSlot = new MetaObject.SlotStr() {
		@Override
		public String str(TypedValue self, Frame<TypedValue> frame) {
			return "<type " + self.as(TypeUserdata.class).name + ">";
		}
	};

	public static final MetaObject.SlotRepr defaultReprSlot = new MetaObject.SlotRepr() {
		@Override
		public String repr(TypedValue self, Frame<TypedValue> frame) {
			return self.as(TypeUserdata.class).name;
		}
	};

	public static SlotAttr defaultAttrSlot(final TypeDomain domain) {
		return new MetaObject.SlotAttr() {
			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				if (ATTR_TYPE_NAME.equals(key)) return Optional.of(domain.create(String.class, self.as(TypeUserdata.class).name));
				return Optional.absent();
			}
		};
	}

	public static MetaObject.Builder defaultMetaObject(final TypeDomain domain) {
		return MetaObject.builder()
				.set(defaultStrSlot)
				.set(defaultReprSlot)
				.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
				.set(defaultAttrSlot(domain));
	}
}