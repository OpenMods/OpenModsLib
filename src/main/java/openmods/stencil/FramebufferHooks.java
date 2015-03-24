package openmods.stencil;

import java.util.Set;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import openmods.Log;
import openmods.utils.render.OpenGLUtils;

public class FramebufferHooks {

	static boolean STENCIL_BUFFER_INJECTED;

	public static void createRenderbufferStorage(int target, int format, int width, int height) {
		OpenGLUtils.flushGlErrors("pre stencil hook");

		if (FramebufferConstants.isStencilBufferEnabled()) {
			OpenGlHelper.func_153186_a(target, FramebufferConstants.DEPTH_STENCIL_FORMAT, width, height);
			Set<Integer> errors = OpenGLUtils.getGlErrors();
			if (errors.isEmpty()) {
				STENCIL_BUFFER_INJECTED = true;
				return;
			}
			Log.warn("Your potato failed to allocate nice buffer. No stencils for you. Cause: %s", OpenGLUtils.errorsToString(errors));
		} else {
			Log.warn("Packet depth+stencil buffer not supported");
		}

		OpenGlHelper.func_153186_a(target, format, width, height);
	}

	public static void attachRenderbuffer(Framebuffer fbo) {
		if (STENCIL_BUFFER_INJECTED) {
			OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e, FramebufferConstants.GL_STENCIL_ATTACHMENT, OpenGlHelper.field_153199_f, fbo.depthBuffer);
		}
	}

	public static void init() {
		FramebufferConstants.init();
	}

}
