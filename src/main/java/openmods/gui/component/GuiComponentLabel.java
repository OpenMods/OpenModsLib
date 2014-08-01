package openmods.gui.component;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import openmods.api.IValueReceiver;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class GuiComponentLabel extends BaseComponent implements IValueReceiver<String> {

	private String text;
	private float scale = 1f;
	private List<String> formattedText;
	private int maxHeight;
	private int maxWidth;
	private float additionalScale = 1.0f;
	private int additionalLineHeight = 0;
	private List<String> tooltip;

	private static FontRenderer getFontRenderer() {
		return Minecraft.getMinecraft().fontRenderer;
	}

	public GuiComponentLabel(int x, int y, String text) {
		this(x, y, getFontRenderer().getStringWidth(text), getFontRenderer().FONT_HEIGHT, text);
	}

	public GuiComponentLabel(int x, int y, int width, int height, String text) {
		super(x, y);
		this.text = text;
		this.maxHeight = height;
		this.maxWidth = width;
	}

	@SuppressWarnings("unchecked")
	public List<String> getFormattedText(FontRenderer fr) {
		if (formattedText == null) {
			if (Strings.isNullOrEmpty(text)) formattedText = ImmutableList.of();
			else formattedText = ImmutableList.copyOf(fr.listFormattedStringToWidth(text, getMaxWidth()));
		}
		return formattedText;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
		additionalScale = sr.getScaleFactor() % 2 == 1 && scale < 1f? 0.667f : 1f;
		if (getMaxHeight() < minecraft.fontRenderer.FONT_HEIGHT) return;
		if (getMaxWidth() < minecraft.fontRenderer.getCharWidth('m')) return;
		GL11.glPushMatrix();
		GL11.glTranslated(offsetX + x, offsetY + y, 1);
		GL11.glScalef(scale * additionalScale, scale * additionalScale, scale * additionalScale);
		int offset = 0;
		int lineCount = 0;
		for (String s : getFormattedText(minecraft.fontRenderer)) {
			if (s == null) break;
			minecraft.fontRenderer.drawString(s, 0, offset, 4210752);
			offset += getFontHeight();
			if (++lineCount >= getMaxLines()) break;
		}
		GL11.glPopMatrix();
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (tooltip != null && !tooltip.isEmpty()) {
			drawHoveringText(tooltip, offsetX + mouseX, offsetY + mouseY, minecraft.fontRenderer);
		}
	}

	private int calculateHeight() {
		FontRenderer fr = getFontRenderer();
		int offset = 0;
		int lineCount = 0;
		for (String s : getFormattedText(fr)) {
			if (s == null) break;
			offset += getFontHeight();
			if (++lineCount >= getMaxLines()) break;
		}
		return offset;
	}

	private int calculateWidth() {
		FontRenderer fr = getFontRenderer();
		int maxWidth = 0;
		for (String s : getFormattedText(fr)) {
			if (s == null) break;
			int width = fr.getStringWidth(s);
			if (width > maxWidth) maxWidth = width;
		}
		return maxWidth;
	}

	public GuiComponentLabel setScale(float scale) {
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
		return getFontRenderer().FONT_HEIGHT + additionalLineHeight;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public GuiComponentLabel setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}

	public int getMaxLines() {
		return (int)Math.floor(getMaxHeight() / (scale / additionalScale) / getFontHeight());
	}

	public int getMaxWidth() {
		return (int)(this.maxWidth / additionalScale);
	}

	@Override
	public int getHeight() {
		return (int)(Math.min(getMaxHeight(), calculateHeight()) + 0.5);
	}

	@Override
	public int getWidth() {
		return (int)(Math.min(getMaxWidth(), calculateWidth()) + 0.5);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.formattedText = null;
		this.text = Strings.nullToEmpty(text);
	}

	public boolean isOverflowing() {
		FontRenderer fr = getFontRenderer();
		return getFormattedText(fr).size() > getMaxLines();
	}

	public void setTooltip(List<String> tooltip) {
		this.tooltip = tooltip;
	}

	public void clearTooltip() {
		this.tooltip = null;
	}

	@Override
	public void setValue(String value) {
		setText(value);
	}
}
