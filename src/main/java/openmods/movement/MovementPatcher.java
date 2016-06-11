package openmods.movement;

import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;
import openmods.asm.StopTransforming;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class MovementPatcher extends ClassVisitor {

	private static final String MANAGER_CLASS = Type.getInternalName(PlayerMovementManager.class);

	private final Method callbackMethod;
	private final MethodMatcher injectedMethodMatcher;
	private final MethodMatcher calledMethodMatcher;
	private final IResultListener listener;

	public MovementPatcher(String obfClassName, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM5, cv);
		this.listener = listener;

		MappedType movementInput = MappedType.of("net/minecraft/util/MovementInput");
		MappedType entityPlayer = MappedType.of("net/minecraft/entity/player/EntityPlayer");

		calledMethodMatcher = new MethodMatcher(movementInput, "()V", "updatePlayerMoveState", "func_78898_a");
		injectedMethodMatcher = new MethodMatcher(obfClassName, "()V", "onLivingUpdate", "func_70636_d");

		callbackMethod = new Method("updateMovementState", Type.VOID_TYPE, ArrayUtils.toArray(movementInput.type(), entityPlayer.type()));
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return injectedMethodMatcher.match(name, desc)? new CallInjector(parent) : parent;
	}

	private class CallInjector extends MethodVisitor {
		public CallInjector(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean intf) {
			boolean patch = opcode == Opcodes.INVOKEVIRTUAL && calledMethodMatcher.match(name, desc);
			if (patch) {
				if (PlayerMovementManager.callbackInjected) {
					Log.warn("Method code mismatch, aborting");
					PlayerMovementManager.callbackInjected = false;
					listener.onFailure();
					throw new StopTransforming();
				}
				visitInsn(Opcodes.DUP); // duplicate movement handler
			}

			super.visitMethodInsn(opcode, owner, name, desc, intf);

			if (patch) {
				// movement handler still on stack
				visitVarInsn(Opcodes.ALOAD, 0); // load this
				visitMethodInsn(Opcodes.INVOKESTATIC, MANAGER_CLASS, callbackMethod.getName(), callbackMethod.getDescriptor(), false);
				Log.debug("Callback inserted. Using new movement handler.");
				listener.onSuccess();
				PlayerMovementManager.callbackInjected = true;
			}
		}
	}

}
