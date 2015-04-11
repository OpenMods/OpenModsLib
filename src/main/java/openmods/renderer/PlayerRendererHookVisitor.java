package openmods.renderer;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraftforge.common.MinecraftForge;
import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import com.google.common.base.Throwables;

@SuppressWarnings("deprecation")
public class PlayerRendererHookVisitor extends ClassVisitor {

	private class InjectorMethodVisitor extends MethodVisitor {

		private final Method postMethod;

		public InjectorMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);

			try {
				postMethod = Method.getMethod(PlayerRendererHookVisitor.class.getMethod("post", AbstractClientPlayer.class, float.class));
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}

			Log.debug("Injecting hook %s.%s into EntityPlayerRender.rotateCorpse", PlayerRendererHookVisitor.class, postMethod);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.RETURN) {
				final Type hookCls = Type.getType(PlayerRendererHookVisitor.class);
				visitVarInsn(Opcodes.ALOAD, 1);
				visitVarInsn(Opcodes.FLOAD, 4);
				visitMethodInsn(Opcodes.INVOKESTATIC, hookCls.getInternalName(), postMethod.getName(), postMethod.getDescriptor());
				listener.onSuccess();
			}

			super.visitInsn(opcode);
		}
	}

	public static void post(AbstractClientPlayer player, float partialTickTime) {
		MinecraftForge.EVENT_BUS.post(new PlayerBodyRenderEvent(player, partialTickTime));
	}

	private final IResultListener listener;

	private final MethodMatcher modifiedMethod;

	public PlayerRendererHookVisitor(String rendererTypeCls, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM4, cv);
		this.listener = listener;

		Type injectedMethodType = Type.getMethodType(Type.VOID_TYPE, MappedType.of(AbstractClientPlayer.class).type(), Type.FLOAT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE);
		modifiedMethod = new MethodMatcher(rendererTypeCls, injectedMethodType.getDescriptor(), "rotateCorpse", "func_77043_a");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return modifiedMethod.match(name, desc)? new InjectorMethodVisitor(parent) : parent;
	}

}
