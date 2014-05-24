package openmods.movement;

import openmods.Log;
import openmods.asm.MethodMatcher;
import openmods.asm.StopTransforming;
import openmods.asm.VisitorHelper;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class MovementPatcher extends ClassVisitor {

	private static final String MANAGER_CLASS = Type.getInternalName(PlayerMovementManager.class);

	private final Method callbackMethod;
	private final MethodMatcher injectedMethodMatcher;
	private final MethodMatcher calledMethodMatcher;

	public MovementPatcher(String obfClassName, ClassVisitor cv) {
		super(Opcodes.ASM4, cv);

		String movementInputName = "net/minecraft/util/MovementInput";
		String entityPlayerName = "net/minecraft/entity/player/EntityPlayer";

		if (VisitorHelper.useSrgNames()) {
			movementInputName = FMLDeobfuscatingRemapper.INSTANCE.unmap(movementInputName);
			entityPlayerName = FMLDeobfuscatingRemapper.INSTANCE.unmap(entityPlayerName);
		}

		Type movementInputType = Type.getObjectType(movementInputName);
		Type entityPlayerType = Type.getObjectType(entityPlayerName);

		callbackMethod = new Method("updateMovementState", Type.VOID_TYPE, ArrayUtils.toArray(movementInputType, entityPlayerType));
		calledMethodMatcher = new MethodMatcher(movementInputName, "()V", "updatePlayerMoveState", "func_78898_a");
		injectedMethodMatcher = new MethodMatcher(obfClassName, "()V", "onLivingUpdate", "func_70636_d");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return injectedMethodMatcher.match(name, desc)? new CallInjector(parent) : parent;
	}

	private class CallInjector extends MethodVisitor {
		public CallInjector(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			boolean patch = opcode == Opcodes.INVOKEVIRTUAL && calledMethodMatcher.match(name, desc);
			if (patch) {
				if (PlayerMovementManager.callbackInjected) {
					Log.warn("Method code mismatch, aborting");
					PlayerMovementManager.callbackInjected = false;
					throw new StopTransforming();
				}
				visitInsn(Opcodes.DUP); // duplicate movement handler
			}

			super.visitMethodInsn(opcode, owner, name, desc);

			if (patch) {
				// movement handler still on stack
				visitVarInsn(Opcodes.ALOAD, 0); // load this
				visitMethodInsn(Opcodes.INVOKESTATIC, MANAGER_CLASS, callbackMethod.getName(), callbackMethod.getDescriptor());
				Log.info("Callback inserted. Using new movement handler.");
				PlayerMovementManager.callbackInjected = true;
			}
		}
	}

}
