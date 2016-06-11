package openmods.stencil;

import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FramebufferInjector extends ClassVisitor {

	private static final MappedType openGlHelper = MappedType.of("net/minecraft/client/renderer/OpenGlHelper");

	private static final MethodMatcher createRenderbufferMatcher;

	private final MethodMatcher targetMethod;

	private static final Type hookType = Type.getType(FramebufferHooks.class);

	private final String className;

	private final IResultListener listener;

	static {
		Type createRenderbufferType = Type.getMethodType(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE);
		createRenderbufferMatcher = new MethodMatcher(openGlHelper, createRenderbufferType.getDescriptor(), "func_153186_a", "func_153186_a");
	}

	private class CreateFramebufferInjector extends MethodVisitor {

		private final Type ownerType;
		private boolean constantFound;

		public CreateFramebufferInjector(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
			this.ownerType = Type.getObjectType(className);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			super.visitLdcInsn(cst);

			if ((cst instanceof Number) && ((Number)cst).intValue() == 0x81A6 /* == 33190 == GL14.GL_DEPTH_COMPONENT24 */) {
				Log.debug("Found GL constant, replacing method");
				constantFound = true;
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean intf) {
			if (constantFound && opcode == Opcodes.INVOKESTATIC && createRenderbufferMatcher.match(name, desc)) {
				Log.debug("Injecting allocate and attach methods");
				super.visitMethodInsn(Opcodes.INVOKESTATIC, hookType.getInternalName(), "createRenderbufferStorage", desc, false);
				super.visitVarInsn(Opcodes.ALOAD, 0);

				Type methodType = Type.getMethodType(Type.VOID_TYPE, ownerType);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, hookType.getInternalName(), "attachRenderbuffer", methodType.getDescriptor(), false);
				listener.onSuccess();
			} else {
				super.visitMethodInsn(opcode, owner, name, desc, intf);
			}

			constantFound = false;
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return targetMethod.match(name, desc)? new CreateFramebufferInjector(parent) : parent;
	}

	public FramebufferInjector(String rawCls, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM5, cv);

		this.listener = listener;

		this.className = rawCls.replace('.', '/');
		Type targetType = Type.getMethodType(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE);
		targetMethod = new MethodMatcher(rawCls, targetType.getDescriptor(), "createFramebuffer", "func_147605_b");
	}

}
