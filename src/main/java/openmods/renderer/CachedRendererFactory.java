package openmods.renderer;

import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glNormalPointer;
import static org.lwjgl.opengl.GL11.glTexCoordPointer;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import openmods.Log;
import org.lwjgl.opengl.GL11;

public class CachedRendererFactory {

	public interface CachedRenderer {
		public void render();

		public void dispose();
	}

	private static class VboRenderer implements CachedRenderer {

		private final VertexBuffer vb;

		private final int drawMode;

		private final Runnable setup;

		private final Runnable cleanup;

		public VboRenderer(Tessellator tes) {
			final BufferBuilder buffer = tes.getBuffer();
			final VertexFormat vf = buffer.getVertexFormat();
			this.vb = new VertexBuffer(vf);
			this.drawMode = buffer.getDrawMode();

			buffer.finishDrawing();
			buffer.reset();
			vb.bufferData(buffer.getByteBuffer());

			Runnable setup = () -> {};
			Runnable cleanup = () -> {};

			final int stride = vf.getNextOffset();

			for (int i = vf.getElementCount() - 1; i >= 0; i--) {
				final VertexFormatElement attr = vf.getElement(i);
				final int offset = vf.getOffset(i);
				final int count = attr.getElementCount();
				final int constant = attr.getType().getGlConstant();
				final int index = attr.getIndex();
				final EnumUsage usage = attr.getUsage();

				final Runnable prevSetup = setup;
				final Runnable prevCleanup = cleanup;

				switch (usage) {
					case POSITION:
						setup = () -> {
							glVertexPointer(count, constant, stride, offset);
							glEnableClientState(GL_VERTEX_ARRAY);
							prevSetup.run();
						};

						cleanup = () -> {
							glDisableClientState(GL_VERTEX_ARRAY);
							prevCleanup.run();
						};
						break;
					case NORMAL:
						Preconditions.checkArgument(count == 3, "Normal attribute %s should have the size 3", attr);
						setup = () -> {
							glNormalPointer(constant, stride, offset);
							glEnableClientState(GL_NORMAL_ARRAY);
							prevSetup.run();
						};

						cleanup = () -> {
							glDisableClientState(GL_NORMAL_ARRAY);
							prevCleanup.run();
						};
						break;
					case COLOR:
						setup = () -> {
							glColorPointer(count, constant, stride, offset);
							glEnableClientState(GL_COLOR_ARRAY);
							prevSetup.run();
						};

						cleanup = () -> {
							glDisableClientState(GL_COLOR_ARRAY);
							GlStateManager.resetColor();
							prevCleanup.run();
						};
						break;
					case UV:
						setup = () -> {
							OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + index);
							glTexCoordPointer(count, constant, stride, offset);
							glEnableClientState(GL_TEXTURE_COORD_ARRAY);
							OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
							prevSetup.run();
						};

						cleanup = () -> {
							OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + index);
							glDisableClientState(GL_TEXTURE_COORD_ARRAY);
							OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
							prevCleanup.run();
						};
						break;
					case PADDING:
						break;
					case GENERIC:
						setup = () -> {
							glEnableVertexAttribArray(index);
							glVertexAttribPointer(index, count, constant, false, stride, offset);
							prevSetup.run();
						};

						cleanup = () -> {
							glDisableVertexAttribArray(index);
							prevCleanup.run();
						};
					default:
						Log.severe("Unimplemented vanilla attribute upload: %s", usage.getDisplayName());
				}
			}

			this.setup = setup;
			this.cleanup = cleanup;
		}

		@Override
		public void render() {
			vb.bindBuffer();

			setup.run();
			vb.drawArrays(drawMode);
			cleanup.run();
			vb.unbindBuffer();
		}

		@Override
		public void dispose() {
			vb.deleteGlBuffers();
		}

	}

	private static class DisplayListRenderer implements CachedRenderer {

		private int displayList = GL11.GL_ZERO;

		private boolean isDisplayListValid() {
			return displayList != GL11.GL_ZERO;
		}

		public DisplayListRenderer(Tessellator tes) {
			displayList = GL11.glGenLists(1);
			if (isDisplayListValid()) {
				GL11.glNewList(displayList, GL11.GL_COMPILE);
				tes.draw();
				GL11.glEndList();
			}
		}

		@Override
		public void render() {
			if (isDisplayListValid()) {
				GL11.glCallList(displayList);
			}
		}

		@Override
		public void dispose() {
			if (isDisplayListValid()) {
				GL11.glDeleteLists(displayList, 1);
			}
		}

	}

	public CachedRenderer createRenderer(Tessellator tes) {
		if (OpenGlHelper.useVbo()) {
			return new VboRenderer(tes);
		} else {
			return new DisplayListRenderer(tes);
		}
	}

}
