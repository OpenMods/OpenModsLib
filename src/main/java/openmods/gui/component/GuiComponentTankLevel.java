package openmods.gui.component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueReceiver;
import openmods.gui.misc.BoxRenderer;
import openmods.utils.MiscUtils;
import org.lwjgl.opengl.GL11;

public class GuiComponentTankLevel extends GuiComponentResizable {

	private static final BoxRenderer BOX_RENDERER = new BoxRenderer(0, 0);
	private static final int BORDER_COLOR = 0xc6c6c6;

	private FluidStack fluidStack;

	private int capacity;

	private boolean displayFluidName = true;

	public GuiComponentTankLevel(int x, int y, int width, int height, int capacity) {
		super(x, y, width, height);
		this.capacity = capacity;
	}

	private static void addVertexWithUV(double x, double y, double z, float u, float v) {
		GL11.glTexCoord2f(u, v);
		GL11.glVertex3d(x, y, z);
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		bindComponentsSheet();
		BOX_RENDERER.render(this, x + offsetX, y + offsetY, width, height, BORDER_COLOR);

		if (fluidStack == null) return;
		final Fluid fluid = fluidStack.getFluid();
		if (fluid == null) return;

		parent.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

		final ResourceLocation textureLocation = fluid.getStill(fluidStack);
		TextureAtlasSprite icon = parent.getIcon(textureLocation);

		if (icon != null) {
			double percentFull = Math.max(0, Math.min(1, (double)fluidStack.amount / (double)capacity));
			double fluidHeight = (height - 3) * percentFull;
			final int posX = offsetX + x;
			final int posY = offsetY + y;

			final float minU = icon.getMinU();
			final float maxU = icon.getMaxU();

			final float minV = icon.getMinV();
			final float maxV = icon.getMaxV();

			GL11.glBegin(GL11.GL_QUADS);
			addVertexWithUV(posX + 3, posY + height - 3, this.zLevel, minU, maxV);
			addVertexWithUV(posX + width - 3, posY + height - 3, this.zLevel, maxU, maxV);
			addVertexWithUV(posX + width - 3, posY + (height - fluidHeight), this.zLevel, maxU, minV);
			addVertexWithUV(posX + 3, posY + (height - fluidHeight), this.zLevel, minU, minV);
			GL11.glEnd();
		}
	}

	@Override
	public void renderOverlay(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (fluidStack != null && isMouseOver(mouseX, mouseY)) {
			final List<String> lines = Lists.newArrayListWithCapacity(2);
			if (displayFluidName) {
				final String translatedFluidName = MiscUtils.getTranslatedFluidName(fluidStack);
				if (!Strings.isNullOrEmpty(translatedFluidName))
					lines.add(translatedFluidName);
			}

			lines.add(String.format("%d/%d", fluidStack.amount, capacity));
			parent.drawHoveringText(lines, offsetX + mouseX, offsetY + mouseY);
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
