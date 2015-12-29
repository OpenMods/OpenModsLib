package openmods.gui.component.page;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import openmods.gui.IComponentParent;
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

	public ItemStackTocPage(IComponentParent parent, int rows, int columns, float iconScale) {
		super(parent);
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

		final GuiComponentItemStack component = new GuiComponentItemStack(parent, x, y, stack, true, iconScale);
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
