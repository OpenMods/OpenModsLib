package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.types.multi.MetaObject.SlotAttr;

class TypeUserdata {
	public static final String ATTR_TYPE_NAME = "name";
	public static final String ATTR_TYPE_METAOBJECT = "metaobject";

	public final String name;

	public final Class<?> type;

	public TypeUserdata(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return "<type: " + name + " " + type + ">";
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
				return self.as(TypeUserdata.class).attr(domain, key);
			}
		};
	}

	protected Optional<TypedValue> attr(TypeDomain domain, String key) {
		if (ATTR_TYPE_NAME.equals(key)) return Optional.of(domain.create(String.class, name));
		if (ATTR_TYPE_METAOBJECT.equals(key)) return Optional.of(domain.create(MetaObject.class, domain.getDefaultMetaObject(type)));

		return Optional.absent();
	}

	public static MetaObject.Builder defaultMetaObject(TypeDomain domain) {
		return MetaObject.builder()
				.set(defaultStrSlot)
				.set(defaultReprSlot)
				.set(MetaObjectUtils.USE_VALUE_EQUALS)
				.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
				.set(defaultAttrSlot(domain));
	}
}