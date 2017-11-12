package openmods.renderer;

import com.google.common.base.Preconditions;
import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MethodMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class PreWorldRenderHookVisitor extends ClassVisitor {

	private static boolean active;

	private static Runnable hook = new Runnable() {
		@Override
		public void run() {}
	};

	public static boolean isActive() {
		return active;
	}

	public static void setHook(Runnable hook) {
		Preconditions.checkNotNull(hook);
		PreWorldRenderHookVisitor.hook = hook;
	}

	public static void callHook() {
		hook.run();
	}

	private final IResultListener listener;

	private final MethodMatcher modifiedMethod;

	public PreWorldRenderHookVisitor(String rendererTypeCls, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM5, cv);
		this.listener = listener;

		Type injectedMethodType = Type.getMethodType(Type.VOID_TYPE, Type.INT_TYPE, Type.FLOAT_TYPE, Type.LONG_TYPE);
		modifiedMethod = new MethodMatcher(rendererTypeCls, injectedMethodType.getDescriptor(), "renderWorldPass", "func_175068_a");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return modifiedMethod.match(name, desc)? new InjectorMethodVisitor(parent) : parent;
	}

	private class InjectorMethodVisitor extends MethodVisitor {

		private final Type hookCls;

		private final Method hookMethod;

		private boolean isPrimed;

		public InjectorMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);

			try {
				hookCls = Type.getType(PreWorldRenderHookVisitor.class);
				hookMethod = Method.getMethod(PreWorldRenderHookVisitor.class.getMethod("callHook"));
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}

			Log.debug("Injecting hook %s.%s into EntityRenderer.renderWorldPass", PreWorldRenderHookVisitor.class, hookMethod);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			if ("prepareterrain".equals(cst))
				isPrimed = true;
			super.visitLdcInsn(cst);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean intf) {
			super.visitMethodInsn(opcode, owner, name, desc, intf);

			if (isPrimed) {
				isPrimed = false;

				visitMethodInsn(Opcodes.INVOKESTATIC, hookCls.getInternalName(), hookMethod.getName(), hookMethod.getDescriptor(), false);
				listener.onSuccess();
				active = true;
			}
		}
	}
}
