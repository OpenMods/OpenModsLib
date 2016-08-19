package openmods.entity;

import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class PlayerDamageEventInjector extends ClassVisitor {

	private class InjectorMethodVisitor extends MethodVisitor {

		public InjectorMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);

			if (markerMethod.match(name, desc)) {
				visitVarInsn(Opcodes.ALOAD, 0); // this
				visitVarInsn(Opcodes.ALOAD, 1); // damage source
				visitVarInsn(Opcodes.FLOAD, 2); // amount
				visitMethodInsn(Opcodes.INVOKESTATIC, "openmods/entity/PlayerDamageEvent", "post", injectedMethodType, false);
				visitVarInsn(Opcodes.FSTORE, 2);
				listener.onSuccess();
			}
		}

	}

	private final IResultListener listener;

	private final MethodMatcher modifiedMethod;
	private final MethodMatcher markerMethod;

	private final String injectedMethodType;

	public PlayerDamageEventInjector(String entityPlayerCls, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM5, cv);
		this.listener = listener;

		final Type damageSourceType = MappedType.of("net.minecraft.util.DamageSource").type();
		final String modifiedMethodType = Type.getMethodDescriptor(Type.VOID_TYPE, damageSourceType, Type.FLOAT_TYPE);
		modifiedMethod = new MethodMatcher(entityPlayerCls, modifiedMethodType, "damageEntity", "func_70665_d");
		markerMethod = new MethodMatcher(entityPlayerCls, "(F)V", "setAbsorptionAmount", "func_110149_m");

		injectedMethodType = Type.getMethodDescriptor(Type.FLOAT_TYPE, MappedType.of(entityPlayerCls).type(), damageSourceType, Type.FLOAT_TYPE);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return modifiedMethod.match(name, desc)? new InjectorMethodVisitor(parent) : parent;
	}
}
