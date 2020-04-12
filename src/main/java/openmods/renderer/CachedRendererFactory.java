package openmods.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class CachedRendererFactory {

	public interface CachedRenderer extends AutoCloseable {
		void render(final MatrixStack matrixStack);

		void close();
	}

	private static class VboRenderer implements CachedRenderer {
		private final VertexBuffer vb;

		private final int drawMode;

		private final VertexFormat vf;

		public VboRenderer(int drawMode, Tessellator tes) {
			this.drawMode = drawMode;
			final BufferBuilder inputBuffer = tes.getBuffer();
			inputBuffer.finishDrawing();
			vf = inputBuffer.getVertexFormat();
			vb = new VertexBuffer(vf);

			inputBuffer.reset();
			vb.upload(inputBuffer);
		}

		@Override
		public void render(final MatrixStack matrixStack) {
			vb.bindBuffer();
			vf.setupBufferState(0);
			vb.draw(matrixStack.getLast().getMatrix(), drawMode);
			vf.clearBufferState();
			VertexBuffer.unbindBuffer();
		}

		@Override
		public void close() {
			vb.close();
		}

	}

	public CachedRenderer createRenderer(int drawMode, Tessellator tes) {
		return new VboRenderer(drawMode, tes);
	}

}
