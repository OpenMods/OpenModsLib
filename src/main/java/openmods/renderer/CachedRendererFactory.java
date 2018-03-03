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

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraftforge.fml.common.FMLLog;
import org.lwjgl.opengl.GL11;

public class CachedRendererFactory {

	public interface CachedRenderer {
		public void render();

		public void dispose();
	}

	private static class VboRenderer implements CachedRenderer {

		private final VertexBuffer vb;

		private final VertexFormat vf;

		private final int drawMode;

		public VboRenderer(Tessellator tes) {
			final BufferBuilder buffer = tes.getBuffer();
			this.vf = buffer.getVertexFormat();
			this.vb = new VertexBuffer(vf);
			this.drawMode = buffer.getDrawMode();

			buffer.finishDrawing();
			buffer.reset();
			vb.bufferData(buffer.getByteBuffer());
		}

		@Override
		public void render() {
			vb.bindBuffer();

			setupPointers();
			vb.drawArrays(drawMode);
			clearPointers();
			vb.unbindBuffer();
		}

		private void setupPointers() {
			final int stride = vf.getNextOffset();

			for (int i = 0; i < vf.getElementCount(); i++) {
				final VertexFormatElement attr = vf.getElement(i);
				final int offset = vf.getOffset(i);
				final int count = attr.getElementCount();
				final int constant = attr.getType().getGlConstant();
				final EnumUsage usage = attr.getUsage();

				switch (usage) {
					case POSITION:
						glVertexPointer(count, constant, stride, offset);
						glEnableClientState(GL_VERTEX_ARRAY);
						break;
					case NORMAL:
						if (count != 3) { throw new IllegalArgumentException("Normal attribute should have the size 3: " + attr); }
						glNormalPointer(constant, stride, offset);
						glEnableClientState(GL_NORMAL_ARRAY);
						break;
					case COLOR:
						glColorPointer(count, constant, stride, offset);
						glEnableClientState(GL_COLOR_ARRAY);
						break;
					case UV:
						OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + attr.getIndex());
						glTexCoordPointer(count, constant, stride, offset);
						glEnableClientState(GL_TEXTURE_COORD_ARRAY);
						OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
						break;
					case PADDING:
						break;
					case GENERIC:
						glEnableVertexAttribArray(attr.getIndex());
						glVertexAttribPointer(attr.getIndex(), count, constant, false, stride, offset);
					default:
						FMLLog.log.fatal("Unimplemented vanilla attribute upload: {}", usage.getDisplayName());
				}
			}
		}

		private void clearPointers() {
			for (int i = 0; i < vf.getElementCount(); i++) {
				final VertexFormatElement attr = vf.getElement(i);
				final EnumUsage usage = attr.getUsage();
				switch (usage) {
					case POSITION:
						glDisableClientState(GL_VERTEX_ARRAY);
						break;
					case NORMAL:
						glDisableClientState(GL_NORMAL_ARRAY);
						break;
					case COLOR:
						glDisableClientState(GL_COLOR_ARRAY);
						// is this really needed?
						GlStateManager.resetColor();
						break;
					case UV:
						OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + attr.getIndex());
						glDisableClientState(GL_TEXTURE_COORD_ARRAY);
						OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
						break;
					case PADDING:
						break;
					case GENERIC:
						glDisableVertexAttribArray(attr.getIndex());
					default:
						FMLLog.log.fatal("Unimplemented vanilla attribute upload: {}", usage.getDisplayName());
				}
			}
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
