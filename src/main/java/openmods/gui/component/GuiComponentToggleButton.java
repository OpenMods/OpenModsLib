package openmods.gui.component;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Collection;
import java.util.Map;
import openmods.api.IValueReceiver;
import openmods.gui.Icon;

public class GuiComponentToggleButton<T> extends GuiComponentButton implements IValueReceiver<T> {

	public static final int BORDER_SIZE = 4;

	private final Map<T, Icon> icons;

	private T value;

	public GuiComponentToggleButton(int x, int y, int borderColor, Map<T, Icon> icons) {
		super(x, y, getMaxWidth(icons.values()) + BORDER_SIZE, getMaxHeight(icons.values()) + BORDER_SIZE, borderColor);
		this.icons = ImmutableMap.copyOf(icons);
	}

	private static int getMaxWidth(Collection<Icon> icons) {
		int result = 0;
		for (Icon icon : icons) {
			result = Math.max(result, icon.width);
		}

		return result;
	}

	private static int getMaxHeight(Collection<Icon> icons) {
		int result = 0;
		for (Icon icon : icons) {
			result = Math.max(result, icon.height);
		}

		return result;
	}

	@Override
	protected void renderContents(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY, boolean pressed) {
		int offset = (buttonEnabled && pressed)? 3 : 2;

		final Icon icon = icons.get(value);
		if (icon != null) {
			final int middleX = (width - BORDER_SIZE - icon.width) / 2;
			final int middleY = (height - BORDER_SIZE - icon.height) / 2;
			drawSprite(icon, matrixStack, offsetX + x + offset + middleX, offsetY + y + offset + middleY);
		}
	}

	@Override
	public void setValue(T value) {
		this.value = value;
	}

}
