package openmods.gui.component;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.Style;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueReceiver;
import openmods.gui.misc.BoxRenderer;
import openmods.utils.MiscUtils;
import org.lwjgl.opengl.GL11;

public class GuiComponentTankLevel extends GuiComponentResizable {

	private static final BoxRenderer BOX_RENDERER = new BoxRenderer(0, 0);
	private static final int BORDER_COLOR = 0xc6c6c6;

	private FluidStack fluidStack = FluidStack.EMPTY;

	private int capacity;

	private boolean displayFluidName = true;

	public GuiComponentTankLevel(int x, int y, int width, int height, int capacity) {
		super(x, y, width, height);
		this.capacity = capacity;
	}

	@Override
	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		bindComponentsSheet();
		BOX_RENDERER.render(this, matrixStack, x + offsetX, y + offsetY, width, height, BORDER_COLOR);

		if (fluidStack.isEmpty()) { return; }
		final Fluid fluid = fluidStack.getFluid();

		parent.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

		final ResourceLocation textureLocation = fluid.getAttributes().getStillTexture(fluidStack);
		TextureAtlasSprite icon = parent.getIcon(textureLocation);

		if (icon != null) {
			float percentFull = Math.max(0, Math.min(1, (float)fluidStack.getAmount() / capacity));
			float fluidHeight = (height - 3) * percentFull;
			final int posX = offsetX + x;
			final int posY = offsetY + y;

			final float minU = icon.getMinU();
			final float maxU = icon.getMaxU();

			final float minV = icon.getMinV();
			final float maxV = icon.getMaxV();

			Matrix4f matrix = matrixStack.getLast().getMatrix();
			float blitOffset = getBlitOffset();

			BufferBuilder buffer = Tessellator.getInstance().getBuffer();
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
			buffer.pos(matrix, posX + 3, posY + height - 3, blitOffset).tex(minU, maxV).endVertex();
			buffer.pos(matrix, posX + width - 3, posY + height - 3, blitOffset).tex(maxU, maxV).endVertex();
			buffer.pos(matrix, posX + width - 3, posY + (height - fluidHeight), blitOffset).tex(maxU, minV).endVertex();
			buffer.pos(matrix, posX + 3, posY + (height - fluidHeight), blitOffset).tex(minU, minV).endVertex();
			Tessellator.getInstance().draw();
		}
	}

	@Override
	public void renderOverlay(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (!fluidStack.isEmpty() && isMouseOver(mouseX, mouseY)) {
			final List<IReorderingProcessor> lines = Lists.newArrayListWithCapacity(2);
			if (displayFluidName) {
				final IReorderingProcessor translatedFluidName = MiscUtils.getTranslatedFluidName(fluidStack).func_241878_f();
				lines.add(translatedFluidName);
			}

			lines.add(IReorderingProcessor.fromString(String.format("%d/%d", fluidStack.getAmount(), capacity), Style.EMPTY));
			parent.drawHoveringText(matrixStack, lines, offsetX + mouseX, offsetY + mouseY);
		}
	}

	public void setDisplayFluidNameInTooltip(boolean isEnabled) {
		displayFluidName = isEnabled;
	}

	public void setFluid(FluidStack value) {
		fluidStack = value;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public IValueReceiver<FluidStack> fluidReceiver() {
		return value -> fluidStack = value;
	}

	public IValueReceiver<Integer> capacityReceiver() {
		return value -> capacity = value;
	}

}
