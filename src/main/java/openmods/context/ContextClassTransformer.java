package openmods.context;

import java.util.Collection;
import java.util.List;

import openmods.Log;
import openmods.asm.MethodMatcher;

import org.objectweb.asm.*;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("deprecation")
public class ContextClassTransformer extends ClassVisitor {

	private static class WrappingMethodVisitor extends MethodVisitor {

		public WrappingMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitCode() {
			super.visitCode();
			super.visitMethodInsn(Opcodes.INVOKESTATIC, CONTEXT_MANAGER.getInternalName(), "push", "()V");
		}

		@Override
		public void visitInsn(int opcode) {
			if (isMethodExit(opcode)) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, CONTEXT_MANAGER.getInternalName(), "pop", "()V");
			}
			super.visitInsn(opcode);
		}

	}

	private static final Type CONTEXT_MANAGER = Type.getType(ContextManager.class);

	private static boolean isMethodExit(int opcode) {
		switch (opcode) {
			case Opcodes.ATHROW:
			case Opcodes.IRETURN:
			case Opcodes.FRETURN:
			case Opcodes.ARETURN:
			case Opcodes.LRETURN:
			case Opcodes.DRETURN:
			case Opcodes.RETURN:
				return true;
			default:
				return false;
		}
	}

	private final List<MethodMatcher> methods;

	private boolean shouldWrap(String name, String desc) {
		for (MethodMatcher matcher : methods)
			if (matcher.match(name, desc)) return true;

		return false;
	}

	public ContextClassTransformer(ClassVisitor cv, Collection<MethodMatcher> matchers) {
		super(Opcodes.ASM5, cv);
		this.methods = ImmutableList.copyOf(matchers);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

		if (shouldWrap(name, desc)) {
			Log.info("Adding context wrapper to %s %s", name, desc);
			return new WrappingMethodVisitor(mv);
		}

		return mv;
	}
}
