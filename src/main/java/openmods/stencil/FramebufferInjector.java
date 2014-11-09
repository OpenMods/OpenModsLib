package openmods.stencil;

import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;

import org.objectweb.asm.*;

@SuppressWarnings("deprecation")
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
			super(Opcodes.ASM4, mv);
			this.ownerType = Type.getObjectType(className);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			super.visitLdcInsn(cst);

			if ((cst instanceof Number) && ((Number)cst).intValue() == 0x81A6 /* == 33190 == GL14.GL_DEPTH_COMPONENT24 */) {
				Log.info("Found GL constant, replacing method");
				constantFound = true;
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			if (constantFound && opcode == Opcodes.INVOKESTATIC && createRenderbufferMatcher.match(name, desc)) {
				Log.info("Injecting allocate and attach methods");
				super.visitMethodInsn(Opcodes.INVOKESTATIC, hookType.getInternalName(), "createRenderbufferStorage", desc);
				super.visitVarInsn(Opcodes.ALOAD, 0);

				Type methodType = Type.getMethodType(Type.VOID_TYPE, ownerType);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, hookType.getInternalName(), "attachRenderbuffer", methodType.getDescriptor());
				listener.onSuccess();
			} else {
				super.visitMethodInsn(opcode, owner, name, desc);
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
		super(Opcodes.ASM4, cv);

		this.listener = listener;

		this.className = rawCls.replace('.', '/');
		Type targetType = Type.getMethodType(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE);
		targetMethod = new MethodMatcher(rawCls, targetType.getDescriptor(), "createFramebuffer", "func_147605_b");
	}

}
