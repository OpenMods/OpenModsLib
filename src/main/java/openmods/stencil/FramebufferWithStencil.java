package openmods.stencil;

import net.minecraft.client.shader.Framebuffer;

public class FramebufferWithStencil extends FramebufferBase {

	public boolean attachToFramebuffer(Framebuffer fbo, int format) {
		if (isAllocated()) {
			if (isDepthBufferUsed(fbo.depthBuffer) && isTextureUsed(fbo.framebufferTexture)) return true;
			deallocate();
		}

		useExternalDepthBuffer(fbo.depthBuffer);
		useExternalTexture(fbo.framebufferTexture);
		allocateStencilBuffer(format, fbo.framebufferTextureWidth, fbo.framebufferTextureHeight);

		if (!allocate()) {
			deallocate();
			return false;
		}

		return true;
	}
}
