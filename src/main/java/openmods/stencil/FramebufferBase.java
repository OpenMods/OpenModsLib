package openmods.stencil;

import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;

public class FramebufferBase {

	private int framebufferObject = -1;

	private int allocatedDepthBuffer = -1;
	private int allocatedStencilBuffer = -1;
	private int allocatedTexture = -1;

	private int usedDepthBuffer = -1;
	private int usedStencilBuffer = -1;
	private int usedTexture = -1;

	protected void useExternalTexture(int texture) {
		Preconditions.checkState(allocatedTexture == -1, "Trying to change texture, but one is already allocated");
		this.usedTexture = texture;
	}

	protected boolean isTextureUsed(int texture) {
		return usedTexture == texture;
	}

	protected void useExternalStencilBuffer(int buffer) {
		Preconditions.checkState(allocatedStencilBuffer == -1, "Trying to change stencil buffer, but one is already allocated");
		this.usedStencilBuffer = buffer;
	}

	protected boolean isStencilBufferUsed(int buffer) {
		return usedStencilBuffer == buffer;
	}

	protected void useExternalDepthBuffer(int buffer) {
		Preconditions.checkState(allocatedDepthBuffer == -1, "Trying to change depth buffer, but one is already allocated");
		this.usedDepthBuffer = buffer;
	}

	protected boolean isDepthBufferUsed(int buffer) {
		return usedDepthBuffer == buffer;
	}

	protected void allocateStencilBuffer(int format, int width, int height) {
		Preconditions.checkState(this.allocatedStencilBuffer == -1, "Stencil buffer already allocated");
		// OpenGlHelper.createRenderbuffer()
		this.usedStencilBuffer = this.allocatedStencilBuffer = OpenGlHelper.func_153185_f();

		// OpenGlHelper.bindRenderbuffer(OpenGlHelper.GL_RENDERBUFFER, this.stencilBuffer);
		OpenGlHelper.func_153176_h(OpenGlHelper.field_153199_f, this.allocatedStencilBuffer);

		// OpenGlHelper.createRenderbufferStorage(OpenGlHelper.GL_RENDERBUFFER, FramebufferConstants.GL_STENCIL_FORMAT, this.framebufferTextureWidth, this.framebufferTextureHeight);
		OpenGlHelper.func_153186_a(OpenGlHelper.field_153199_f, format, width, height);
	}

	public boolean isAllocated() {
		return framebufferObject > -1;
	}

	public void deallocate() {
		if (!OpenGlHelper.isFramebufferEnabled()) return;

		unbindFramebuffer();

		if (this.allocatedDepthBuffer > -1) {
			OpenGlHelper.func_153184_g(this.allocatedDepthBuffer);
			this.allocatedDepthBuffer = -1;
		}

		if (this.allocatedStencilBuffer > -1) {
			OpenGlHelper.func_153184_g(this.allocatedStencilBuffer);
			this.allocatedStencilBuffer = -1;
		}

		if (this.allocatedTexture > -1) {
			TextureUtil.deleteTexture(this.allocatedTexture);
			this.allocatedTexture = -1;
		}

		if (this.framebufferObject > -1) {
			OpenGlHelper.func_153174_h(this.framebufferObject);
			this.framebufferObject = -1;
			this.usedDepthBuffer = -1;
			this.usedStencilBuffer = -1;
			this.usedTexture = -1;
		}
	}

	protected boolean allocate() {
		if (!OpenGlHelper.isFramebufferEnabled()) return false;

		Preconditions.checkState(usedTexture > -1, "Texture not selected");

		Preconditions.checkState(this.framebufferObject == -1, "Framebuffer already allocated");
		this.framebufferObject = OpenGlHelper.func_153165_e();

		bindFramebuffer();
		// OpenGlHelper.attachFramebufferTexture(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.framebufferTexture, 0);
		OpenGlHelper.func_153188_a(OpenGlHelper.field_153198_e, OpenGlHelper.field_153200_g, GL11.GL_TEXTURE_2D, usedTexture, 0);

		if (usedDepthBuffer > -1) {
			// OpenGlHelper.attachRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
			OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e, OpenGlHelper.field_153201_h, OpenGlHelper.field_153199_f, usedDepthBuffer);
		}

		if (usedStencilBuffer > -1) {
			// OpenGlHelper.attachRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, FramebufferConstants.GL_STENCIL_ATTACHMENT, OpenGlHelper.GL_RENDERBUFFER, this.stencilBuffer);
			OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e, FramebufferConstants.GL_STENCIL_ATTACHMENT, OpenGlHelper.field_153199_f, usedStencilBuffer);
		}

		// OpenGlHelper.checkFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER);
		int fboStatus = OpenGlHelper.func_153167_i(OpenGlHelper.field_153198_e);

		unbindFramebuffer();

		boolean result = FramebufferConstants.checkFramebufferComplete(fboStatus);

		if (!result) {
			OpenGlHelper.func_153174_h(this.framebufferObject);
			this.framebufferObject = -1;
		}

		return result;
	}

	public boolean bindFramebuffer() {
		if (!OpenGlHelper.isFramebufferEnabled()) return false;

		Preconditions.checkState(framebufferObject > -1, "FBO not initialized");
		// OpenGlHelper.bindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
		OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, this.framebufferObject);
		return true;
	}

	public void unbindFramebuffer() {
		if (!OpenGlHelper.isFramebufferEnabled()) return;

		// OpenGlHelper.bindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
		OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, 0);
	}
}
