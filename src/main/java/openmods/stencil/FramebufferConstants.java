package openmods.stencil;

import net.minecraft.client.renderer.OpenGlHelper;
import openmods.Log;

import org.lwjgl.opengl.*;

public class FramebufferConstants {
	public static int GL_STENCIL_ATTACHMENT;
	public static int GL_FRAMEBUFFER_UNSUPPORTED;
	public static int GL_FRAMEBUFFER_UNDEFINED;

	public static int DEPTH_STENCIL_FORMAT = -1;

	public static void init() {
		ContextCapabilities capabilities = GLContext.getCapabilities();

		if (capabilities.OpenGL30) {
			GL_STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;
			GL_FRAMEBUFFER_UNSUPPORTED = GL30.GL_FRAMEBUFFER_UNSUPPORTED;
			GL_FRAMEBUFFER_UNDEFINED = GL30.GL_FRAMEBUFFER_UNDEFINED;
			DEPTH_STENCIL_FORMAT = GL30.GL_DEPTH24_STENCIL8;
		} else if (capabilities.GL_ARB_framebuffer_object) {
			GL_STENCIL_ATTACHMENT = ARBFramebufferObject.GL_STENCIL_ATTACHMENT;
			GL_FRAMEBUFFER_UNSUPPORTED = ARBFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED;
			GL_FRAMEBUFFER_UNDEFINED = ARBFramebufferObject.GL_FRAMEBUFFER_UNDEFINED;
			DEPTH_STENCIL_FORMAT = ARBFramebufferObject.GL_DEPTH24_STENCIL8;
		} else if (capabilities.GL_EXT_framebuffer_object) {
			GL_STENCIL_ATTACHMENT = EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT;
			GL_FRAMEBUFFER_UNSUPPORTED = EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT;
			GL_FRAMEBUFFER_UNDEFINED = -1; // missing
			DEPTH_STENCIL_FORMAT = capabilities.GL_EXT_packed_depth_stencil? EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT : -1;
		} else {
			throw new IllegalArgumentException("Unable to determine capabilities");
		}
	}

	private static boolean logError(String name) {
		Log.warn("FBO not complete: %s", name);
		return false;
	}

	public static boolean checkFramebufferComplete(int fboStatus) {
		if (fboStatus == OpenGlHelper.field_153202_i) return true;
		if (fboStatus == OpenGlHelper.field_153203_j) return logError("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
		if (fboStatus == OpenGlHelper.field_153205_l) return logError("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
		if (fboStatus == OpenGlHelper.field_153206_m) return logError("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
		if (fboStatus == GL_FRAMEBUFFER_UNSUPPORTED) return logError("GL_FRAMEBUFFER_UNSUPPORTED");
		if (fboStatus == GL_FRAMEBUFFER_UNDEFINED) return logError("GL_FRAMEBUFFER_UNDEFINED");

		return logError(String.format("unknown 0x%04X", fboStatus));
	}
}
