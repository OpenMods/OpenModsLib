package openmods.utils.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import openmods.Log;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

public abstract class FramebufferBlitter {

	public static FramebufferBlitter INSTANCE = new FramebufferBlitter() {

		@Override
		protected void blitFramebufferOp(Framebuffer in, Framebuffer out) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected int getReadConst() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected int getDrawConst() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isValid() {
			return false;
		}
	};

	private static class GL30Impl extends FramebufferBlitter {

		@Override
		public void blitFramebufferOp(Framebuffer in, Framebuffer out) {
			GL30.glBlitFramebuffer(
					0, 0, in.framebufferWidth, in.framebufferHeight,
					0, 0, out.framebufferWidth, out.framebufferHeight,
					GL11.GL_COLOR_BUFFER_BIT,
					GL11.GL_NEAREST);

		}

		@Override
		protected int getReadConst() {
			return GL30.GL_READ_FRAMEBUFFER;
		}

		@Override
		protected int getDrawConst() {
			return GL30.GL_DRAW_FRAMEBUFFER;
		}

		@Override
		public boolean isValid() {
			return true;
		}

	}

	private static class ExtImpl extends FramebufferBlitter {

		@Override
		public void blitFramebufferOp(Framebuffer in, Framebuffer out) {
			EXTFramebufferBlit.glBlitFramebufferEXT(
					0, 0, in.framebufferWidth, in.framebufferHeight,
					0, 0, out.framebufferWidth, out.framebufferHeight,
					GL11.GL_COLOR_BUFFER_BIT,
					GL11.GL_NEAREST);
		}

		@Override
		protected int getReadConst() {
			return EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT;
		}

		@Override
		protected int getDrawConst() {
			return EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT;
		}

		@Override
		public boolean isValid() {
			return true;
		}

	}

	public static boolean setup() {
		if (OpenGlHelper.framebufferSupported) {
			final ContextCapabilities caps = GLContext.getCapabilities();

			if (caps.OpenGL30) {
				Log.debug("Using OpenGL 3.0 FB blit");
				INSTANCE = new GL30Impl();
				return true;
			}

			if (caps.GL_EXT_framebuffer_blit) {
				Log.debug("Using EXT FB blit");
				INSTANCE = new ExtImpl();
				return true;
			}
		}

		Log.debug("FB blit not supported");
		return false;
	}

	public void blitFramebuffer(Framebuffer in, Framebuffer out) {
		OpenGlHelper.glBindFramebuffer(getReadConst(), in.framebufferObject);
		OpenGlHelper.glBindFramebuffer(getDrawConst(), out.framebufferObject);

		blitFramebufferOp(in, out);

		OpenGlHelper.glBindFramebuffer(getReadConst(), 0);
		OpenGlHelper.glBindFramebuffer(getDrawConst(), 0);
	}

	protected abstract void blitFramebufferOp(Framebuffer in, Framebuffer out);

	protected abstract int getReadConst();

	protected abstract int getDrawConst();

	public abstract boolean isValid();

}
