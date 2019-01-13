package openmods.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import openmods.utils.CachedFactory;
import openmods.utils.SneakyThrower;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class ClonerFactory implements Opcodes {

	private static class ClonerClassLoader extends ClassLoader {
		private ClonerClassLoader() {
			super(ClonerClassLoader.class.getClassLoader());
		}

		public Class<?> define(byte[] data) {
			return defineClass(null, data, 0, data.length);
		}
	}

	public interface ICloner<T> {
		<A extends T, B extends T> void clone(A from, B to);
	}

	private static final String CLONER_DESC = Type.getInternalName(ICloner.class);

	private static final Method CLONER_FUNC_DESC = Method.getMethod(ICloner.class.getDeclaredMethods()[0]);

	public static final ClonerFactory instance = new ClonerFactory();

	private final CachedFactory<Class<?>, ICloner<?>> cache = new CachedFactory<Class<?>, ClonerFactory.ICloner<?>>() {
		@Override
		protected ICloner<?> create(Class<?> key) {
			try {
				Class<? extends ICloner<?>> cls = createClonerClass(key);
				return cls.newInstance();
			} catch (Throwable t) {
				throw SneakyThrower.sneakyThrow(t);
			}
		}
	};

	private final ClonerClassLoader clonerClassLoader = new ClonerClassLoader();

	private static byte[] createClonerClassData(Class<?> cls) {
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		final String commonCls = Type.getInternalName(cls);

		final String name = commonCls + "$$cloner$";

		writer.visit(V1_6, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, name, null, "java/lang/Object", new String[] { CLONER_DESC });
		writer.visitSource(".dynamic", null);

		{
			MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		{

			MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, CLONER_FUNC_DESC.getName(), CLONER_FUNC_DESC.getDescriptor(), null, null);
			mv.visitCode();

			mv.visitVarInsn(Opcodes.ALOAD, 2);
			mv.visitTypeInsn(Opcodes.CHECKCAST, commonCls);

			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitTypeInsn(Opcodes.CHECKCAST, commonCls);

			Class<?> currentCls = cls;
			while (currentCls != Object.class) {
				addClonedFields(mv, currentCls);
				currentCls = currentCls.getSuperclass();
			}

			mv.visitInsn(Opcodes.POP2);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		writer.visitEnd();

		return writer.toByteArray();
	}

	private static void addClonedFields(MethodVisitor writer, Class<?> currentCls) {
		final String owner = Type.getType(currentCls).getInternalName();

		for (Field f : currentCls.getDeclaredFields()) {
			final int modifier = f.getModifiers();
			if (Modifier.isFinal(modifier) || Modifier.isStatic(modifier) || !Modifier.isPublic(modifier)) continue;
			final String fieldDesc = Type.getType(f.getType()).getDescriptor();

			writer.visitInsn(Opcodes.DUP2);
			writer.visitFieldInsn(Opcodes.GETFIELD, owner, f.getName(), fieldDesc);
			writer.visitFieldInsn(Opcodes.PUTFIELD, owner, f.getName(), fieldDesc);
		}

	}

	@SuppressWarnings("unchecked")
	private Class<? extends ICloner<?>> createClonerClass(Class<?> cls) {
		final byte[] classData = createClonerClassData(cls);
		return (Class<? extends ICloner<?>>)clonerClassLoader.define(classData);
	}

	@SuppressWarnings("unchecked")
	public <T> ICloner<T> getCloner(Class<T> cls) {
		return (ICloner<T>)cache.getOrCreate(cls);
	}

}
