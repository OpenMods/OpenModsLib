package openmods.utils.render;

import jdk.internal.org.objectweb.asm.Opcodes;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

// just don't ask about this, ok? And yes, it should be in render stuff
public class MarkerClassGenerator {

	private static class BytecodeClassLoader extends ClassLoader {
		private BytecodeClassLoader() {
			super(BytecodeClassLoader.class.getClassLoader());
		}

		public Class<?> define(byte[] data) {
			return defineClass(null, data, 0, data.length);
		}
	}

	public static interface IGeneratedMarkerClass {
		public int getMarkerValue();
	}

	private static final String[] interfaces = new String[] { Type.getInternalName(IGeneratedMarkerClass.class) };

	public static final MarkerClassGenerator instance = new MarkerClassGenerator();

	private final BytecodeClassLoader loader = new BytecodeClassLoader();

	private int counter;

	private <T> Class<? extends T> createMarkerCls(Class<T> superClass, int key) {
		final ClassWriter writer = new ClassWriter(0);

		final String superCls = Type.getInternalName(superClass);
		writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, superCls + "$marker_" + key, null, superCls, interfaces);

		final MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getMarkerValue", "()I", null, null);
		mv.visitCode();
		mv.visitLdcInsn(key);
		mv.visitInsn(Opcodes.IRETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		writer.visitEnd();

		@SuppressWarnings("unchecked")
		final Class<? extends T> cls = (Class<? extends T>)loader.define(writer.toByteArray());
		return cls;
	}

	public <T> Class<? extends T> createMarkerCls(Class<T> superClass) {
		return createMarkerCls(superClass, counter++);
	}

}
