package openmods.gui.component;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.List;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import openmods.api.IValueReceiver;
import openmods.gui.IComponentParent;
import org.lwjgl.opengl.GL11;

public class GuiComponentLabel extends BaseComponent implements IValueReceiver<String> {

	private ITextComponent text;
	private float scale = 1f;
	private List<? extends IReorderingProcessor> formattedText;
	private int maxHeight;
	private int maxWidth;
	private int additionalLineHeight = 0;
	private List<? extends IReorderingProcessor> tooltip;

	public GuiComponentLabel(int x, int y, String text) {
		this(x, y, -1, -1, text);
	}

	public GuiComponentLabel(int x, int y, int width, int height, String text) {
		super(x, y);
		this.text = new StringTextComponent(text);
		this.maxHeight = height;
		this.maxWidth = width;
	}

	@Override
	public void init(IComponentParent parent) {
		super.init(parent);

		if (maxHeight < 0) maxHeight = parent.getFontRenderer().FONT_HEIGHT;
		if (maxWidth < 0) maxWidth = parent.getFontRenderer().func_243245_a(text.func_241878_f());
	}

	public List<? extends IReorderingProcessor> getFormattedText(FontRenderer fr) {
		if (formattedText == null) {
			formattedText = ImmutableList.copyOf(fr.trimStringToWidth(text, getMaxWidth()));
		}
		return formattedText;
	}

	@Override
	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		final FontRenderer fontRenderer = parent.getFontRenderer();

		if (getMaxHeight() < fontRenderer.FONT_HEIGHT) return;
		if (getMaxWidth() < fontRenderer.getStringWidth("m")) return;
		GL11.glPushMatrix();
		GL11.glTranslated(offsetX + x, offsetY + y, 1);
		GL11.glScalef(scale, scale, 1);
		int offset = 0;
		int lineCount = 0;
		for (IReorderingProcessor s : getFormattedText(fontRenderer)) {
			if (s == null) break;
			fontRenderer.func_238422_b_(matrixStack, s, 0, offset, 4210752);
			offset += getFontHeight();
			if (++lineCount >= getMaxLines()) break;
		}
		GL11.glPopMatrix();
	}

	@Override
	public void renderOverlay(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (tooltip != null && !tooltip.isEmpty() && isMouseOver(mouseX, mouseY)) {
			drawHoveringText(matrixStack, tooltip, offsetX + mouseX, offsetY + mouseY);
		}
	}

	private int calculateHeight() {
		final FontRenderer fr = parent.getFontRenderer();
		int offset = 0;
		int lineCount = 0;
		for (IReorderingProcessor s : getFormattedText(fr)) {
			if (s == null) break;
			offset += getFontHeight();
			if (++lineCount >= getMaxLines()) break;
		}
		return offset;
	}

	private int calculateWidth() {
		final FontRenderer fr = parent.getFontRenderer();
		int maxWidth = 0;
		for (IReorderingProcessor s : getFormattedText(fr)) {
			if (s == null) break;
			int width = fr.func_243245_a(s);
			if (width > maxWidth) maxWidth = width;
		}
		return (int)(maxWidth * scale);
	}

	public GuiComponentLabel setScale(float scale) {
		this.formattedText = null;
		this.scale = scale;
		return this;
	}

	public float getScale() {
		return scale;
	}

	public GuiComponentLabel setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		return this;
	}

	public void setAdditionalLineHeight(int lh) {
		this.additionalLineHeight = lh;
	}

	public int getFontHeight() {
		return parent.getFontRenderer().FONT_HEIGHT + additionalLineHeight;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public GuiComponentLabel setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}

	public int getMaxLines() {
		return (int)Math.floor(getMaxHeight() / (scale) / getFontHeight());
	}

	public int getMaxWidth() {
		return (int)(this.maxWidth / scale);
	}

	@Override
	public int getHeight() {
		return (int)(Math.min(getMaxHeight(), calculateHeight()) + 0.5);
	}

	@Override
	public int getWidth() {
		return (int)(Math.min(getMaxWidth(), calculateWidth()) + 0.5);
	}

	public void setText(ITextComponent text) {
		this.formattedText = null;
		this.text = text;
	}

	public boolean isOverflowing() {
		final FontRenderer fr = parent.getFontRenderer();
		return getFormattedText(fr).size() > getMaxLines();
	}

	public void setTooltip(List<? extends IReorderingProcessor> tooltip) {
		this.tooltip = tooltip;
	}

	public void clearTooltip() {
		this.tooltip = null;
	}

	@Override
	public void setValue(String value) {
		setText(new StringTextComponent(value));
	}
}
