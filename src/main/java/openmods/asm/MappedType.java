package openmods.asm;

import org.objectweb.asm.Type;

public class MappedType {

	private final String clsName;

	public static MappedType of(Class<?> cls) {
		return new MappedType(cls.getName());
	}

	public static MappedType of(String clsName) {
		return new MappedType(clsName);
	}

	private MappedType(String clsName) {
		this.clsName = VisitorHelper.getMappedName(clsName.replace('.', '/'));
	}

	public String name() {
		return clsName;
	}

	public Type type() {
		return Type.getObjectType(clsName);
	}

}
