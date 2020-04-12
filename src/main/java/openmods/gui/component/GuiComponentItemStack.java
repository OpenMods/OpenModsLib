package openmods.gui.component;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;

public class GuiComponentItemStack extends BaseComponent {

	@Nonnull
	private final ItemStack stack;

	private final boolean drawTooltip;

	private final float scale;

	private final int size;

	private final List<? extends IReorderingProcessor> displayName;

	public GuiComponentItemStack(int x, int y, @Nonnull ItemStack stack, boolean drawTooltip, float scale) {
		super(x, y);
		this.stack = stack;
		this.drawTooltip = drawTooltip;
		this.scale = scale;

		this.size = MathHelper.floor(16 * scale);
		this.displayName = ImmutableList.of(stack.getDisplayName().func_241878_f());
	}

	@Override
	public int getWidth() {
		return size;
	}

	@Override
	public int getHeight() {
		return size;
	}

	@Override
	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (scale != 1) {
			matrixStack.push();
			matrixStack.scale(scale, scale, 1);
		}
		drawItemStack(stack, (int)((x + offsetX) / scale), (int)((y + offsetY) / scale), "");
		if (scale != 1) { matrixStack.pop(); }
	}

	@Override
	public void renderOverlay(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (drawTooltip && isMouseOver(mouseX, mouseY)) { drawHoveringText(matrixStack, displayName, offsetX + mouseX, offsetY + mouseY); }
	}

}
