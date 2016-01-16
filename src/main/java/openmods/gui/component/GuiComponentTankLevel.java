package openmods.gui.component;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import openmods.api.IValueReceiver;
import openmods.gui.misc.BoxRenderer;

import org.lwjgl.opengl.GL11;

public class GuiComponentTankLevel extends GuiComponentResizable {

	private static final BoxRenderer BOX_RENDERER = new BoxRenderer(0, 0);
	private static final int BORDER_COLOR = 0xc6c6c6;

	private FluidStack fluidStack;

	private int capacity;

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

		parent.bindTexture(TextureMap.locationBlocksTexture);

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

	public void setFluid(FluidStack value) {
		fluidStack = value;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public IValueReceiver<FluidStack> fluidReceiver() {
		return new IValueReceiver<FluidStack>() {
			@Override
			public void setValue(FluidStack value) {
				fluidStack = value;
			}
		};
	}

	public IValueReceiver<Integer> capacityReceiver() {
		return new IValueReceiver<Integer>() {
			@Override
			public void setValue(Integer value) {
				capacity = value;
			}
		};
	}

}
