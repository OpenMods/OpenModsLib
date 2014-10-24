package openmods.asm;

import org.objectweb.asm.Type;

public class MappedType {

	private final String clsName;

	public static MappedType of(Class<?> cls) {
		return new MappedType(cls.getName().replace('.', '/'));
	}

	public static MappedType of(String clsName) {
		return new MappedType(clsName);
	}

	private MappedType(String clsName) {
		this.clsName = VisitorHelper.getMappedName(clsName);
	}

	public String name() {
		return clsName;
	}

	public Type type() {
		return Type.getObjectType(clsName);
	}

}
