package openmods.renderer;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class PlayerRendererHookVisitor extends ClassVisitor {

	private class InjectorMethodVisitor extends MethodVisitor {

		private final Method postMethod;

		public InjectorMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);

			try {
				postMethod = Method.getMethod(PlayerRendererHookVisitor.class.getMethod("post", AbstractClientPlayerEntity.class, float.class));
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}

			Log.debug("Injecting hook %s.%s into EntityPlayerRender.rotateCorpse", PlayerRendererHookVisitor.class, postMethod);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.RETURN) {
				final Type hookCls = Type.getType(PlayerRendererHookVisitor.class);
				visitVarInsn(Opcodes.ALOAD, 1);
				visitVarInsn(Opcodes.FLOAD, 4);
				visitMethodInsn(Opcodes.INVOKESTATIC, hookCls.getInternalName(), postMethod.getName(), postMethod.getDescriptor(), false);
				listener.onSuccess();
			}

			super.visitInsn(opcode);
		}
	}

	public static void post(AbstractClientPlayerEntity player, float partialTickTime) {
		MinecraftForge.EVENT_BUS.post(new PlayerBodyRenderEvent(player, partialTickTime));
	}

	private final IResultListener listener;

	private final MethodMatcher modifiedMethod;

	public PlayerRendererHookVisitor(String rendererTypeCls, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM5, cv);
		this.listener = listener;

		Type injectedMethodType = Type.getMethodType(Type.VOID_TYPE, MappedType.of(AbstractClientPlayerEntity.class).type(), Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE);
		modifiedMethod = new MethodMatcher(rendererTypeCls, injectedMethodType.getDescriptor(), "applyRotations", "func_77043_a");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return modifiedMethod.match(name, desc)? new InjectorMethodVisitor(parent) : parent;
	}

}
