package openmods.gui.component.page;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import openmods.gui.component.GuiComponentItemStack;
import openmods.gui.listener.IMouseDownListener;

public class ItemStackTocPage extends PageBase {

	private int count;

	private int row;

	private int column;

	private final int columns;

	private final int capacity;

	private final int spacerWidth;

	private final int spacerHeight;

	private final float iconScale;

	private final int iconSize;

	public ItemStackTocPage(int rows, int columns, float iconScale) {
		this.capacity = rows * columns;

		this.iconScale = iconScale;
		this.iconSize = MathHelper.floor_float(16 * iconScale);

		this.columns = columns;

		int requiredWidth = iconSize * columns;
		int requiredHeight = iconSize * rows;

		int leftoverWidth = getWidth() - requiredWidth;
		int leftoverHeight = getHeight() - requiredHeight;

		this.spacerWidth = leftoverWidth / (columns - 1);
		this.spacerHeight = leftoverHeight / (rows - 1);
	}

	public int getCapacity() {
		return capacity;
	}

	public boolean addEntry(ItemStack stack, IMouseDownListener clickListener) {
		if (count >= capacity) return false;

		int x = column * (iconSize + spacerWidth);
		int y = row * (iconSize + spacerHeight);

		final GuiComponentItemStack component = new GuiComponentItemStack(x, y, stack, true, iconScale);
		component.setListener(clickListener);
		addComponent(component);

		if (++column >= columns) {
			column = 0;
			row++;
		}

		count++;
		return true;
	}
}
