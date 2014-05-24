package openmods.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.ResourceLocation;
import openmods.utils.render.RenderUtils;

import org.lwjgl.opengl.GL11;

public class StenciledTextureRenderer extends StencilRendererHandler {

	private final ResourceLocation texture;
	private final int stencilMask;

	public StenciledTextureRenderer(int stencilMask, ResourceLocation texture) {
		this.texture = texture;
		this.stencilMask = stencilMask;
	}

	@Override
	public void render(RenderGlobal context, float partialTickTime) {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();

		GL11.glOrtho(-1, +1, -1, +1, -1, +1);

		GL11.glEnable(GL11.GL_STENCIL_TEST);
		GL11.glStencilMask(stencilMask);
		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		GL11.glStencilFunc(GL11.GL_EQUAL, stencilMask, stencilMask);

		// TODO: confirm
		// context.renderEngine.bindTexture(texture);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

		RenderUtils.disableLightmap();
		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glColor3f(1, 1, 1);

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-1, -1, 0);

		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(+1, -1, 0);

		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(+1, +1, 0);

		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(-1, +1, 0);

		GL11.glEnd();

		// mask should prevent this command from clearing other bits
		GL11.glClearStencil(0);
		GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

		GL11.glDisable(GL11.GL_STENCIL_TEST);

		RenderUtils.enableLightmap();
		GL11.glEnable(GL11.GL_LIGHTING);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();

	}

}
