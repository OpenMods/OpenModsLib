package openmods.gui.component;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import openmods.gui.Icon;
import openmods.utils.CollectionUtils;

public class GuiComponentCraftingGrid extends GuiComponentSprite {

	private static final int UPDATE_DELAY = 20;

	private static final Random rnd = new Random();

	private static final Function<ItemStack, ItemStack[]> EXPAND_TRANSFORM = new Function<ItemStack, ItemStack[]>() {
		@Override
		@Nullable
		public ItemStack[] apply(@Nullable ItemStack input) {
			return new ItemStack[] { input.copy() };
		}
	};

	private final ItemStack[][] items;

	private final ItemStack[] selectedItems;

	private int changeCountdown = UPDATE_DELAY;

	public GuiComponentCraftingGrid(int x, int y, ItemStack[] items, Icon background) {
		this(x, y, CollectionUtils.transform(items, EXPAND_TRANSFORM), background);
	}

	public GuiComponentCraftingGrid(int x, int y, ItemStack[][] items, Icon background) {
		super(x, y, background);
		Preconditions.checkNotNull(items, "No items in grid");
		this.items = items;
		this.selectedItems = new ItemStack[items.length];

		selectItems();
	}

	@Override
	public boolean isTicking() {
		return true;
	}

	@Override
	public void tick() {
		if (changeCountdown-- <= 0) {
			selectItems();
			changeCountdown = UPDATE_DELAY;
		}
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		super.render(offsetX, offsetY, mouseX, mouseY);

		final int gridOffsetX = 1;
		final int gridOffsetY = 1;
		final int itemBoxSize = 19;

		for (int i = 0; i < items.length; i++) {
			ItemStack input = selectedItems[i];
			if (!input.isEmpty()) {
				int row = (i % 3);
				int column = i / 3;
				int itemX = offsetX + gridOffsetX + (row * itemBoxSize);
				int itemY = offsetY + gridOffsetY + (column * itemBoxSize);
				drawItemStack(input, x + itemX, y + itemY, "");
			}
		}
	}

	private void selectItems() {
		for (int i = 0; i < items.length; i++) {
			ItemStack[] slotItems = items[i];
			if (slotItems.length == 0) selectedItems[i] = ItemStack.EMPTY;
			else {
				final int choice = rnd.nextInt(slotItems.length);
				selectedItems[i] = slotItems[choice];
			}
		}
	}

	@Override
	public void renderOverlay(int offsetX, int offsetY, int mouseX, int mouseY) {
		super.renderOverlay(offsetX, offsetY, mouseX, mouseY);

		final int relativeMouseX = mouseX + offsetX - x;
		final int relativeMouseY = mouseY + offsetY - y;

		final int gridOffsetX = 1;
		final int gridOffsetY = 1;
		final int itemBoxSize = 19;

		if (isMouseOver(mouseX, mouseY)) {
			ItemStack tooltip = ItemStack.EMPTY;
			// so lazy
			for (int i = 0; i < selectedItems.length; i++) {
				int row = (i % 3);
				int column = i / 3;
				int itemX = offsetX + gridOffsetX + (row * itemBoxSize);
				int itemY = offsetY + gridOffsetY + (column * itemBoxSize);
				if (relativeMouseX > itemX - 2 && relativeMouseX < itemX - 2 + itemBoxSize &&
						relativeMouseY > itemY - 2 && relativeMouseY < itemY - 2 + itemBoxSize) {
					tooltip = selectedItems[i];
					break;
				}
			}

			if (!tooltip.isEmpty()) {
				parent.drawItemStackTooltip(tooltip, relativeMouseX + 25, relativeMouseY + 30);
			}
		}
	}

}
