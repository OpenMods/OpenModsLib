package openmods.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class GuiComponentTextButton extends GuiComponentButton {

	private ITextComponent text;

	public GuiComponentTextButton(int x, int y, int width, int height, int color, ITextComponent text) {
		super(x, y, width, height, color);
		this.text = text;
	}

	public GuiComponentTextButton(int x, int y, int width, int height, int color) {
		this(x, y, width, height, color, StringTextComponent.EMPTY);
	}

	public GuiComponentTextButton setText(ITextComponent buttonText) {
		this.text = buttonText;
		return this;
	}

	@Override
	public void renderContents(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed) {
		final FontRenderer fontRenderer = parent.getFontRenderer();
		int textWidth = fontRenderer.func_243245_a(text.func_241878_f());
		int offX = ((width - textWidth) / 2) + 1;
		int offY = 3;
		if (buttonEnabled && pressed) {
			offY++;
			offX++;
		}
		fontRenderer.func_238422_b_(matrixStack, text.func_241878_f(), offsetX + x + offX, offsetY + y + offY, 4210752);
	}
}
