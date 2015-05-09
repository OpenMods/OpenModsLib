package openmods.gui.component;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import openmods.gui.listener.IValueChangedListener;

import com.google.common.collect.ImmutableList;

public class GuiComponentPalettePicker extends BaseComponent {

	public static class PaletteEntry {
		public final int callback;
		public final int rgb;
		public final String name;

		public PaletteEntry(int callback, int rgb, String name) {
			this.callback = callback;
			this.rgb = rgb;
			this.name = name;
		}
	}

	private List<PaletteEntry> palette = ImmutableList.of();

	private int rowSize = 2;

	private int columnCount;

	private int areaSize = 4;

	private IValueChangedListener<PaletteEntry> listener;

	private boolean drawTooltip = false;

	public GuiComponentPalettePicker(int x, int y) {
		super(x, y);
	}

	@Override
	public int getWidth() {
		return rowSize * areaSize;
	}

	@Override
	public int getHeight() {
		return columnCount * areaSize;
	}

	private void recalculate() {
		if (this.palette.isEmpty()) {
			this.columnCount = 0;
		} else {
			final int count = palette.size();
			this.columnCount = (count + (rowSize - 1)) / rowSize;
		}
	}

	public void setPalette(List<PaletteEntry> colors) {
		this.palette = ImmutableList.copyOf(colors);
		recalculate();
	}

	public void setRowSize(int rowSize) {
		this.rowSize = rowSize;
		recalculate();
	}

	public void setAreaSize(int areaSize) {
		this.areaSize = areaSize;
	}

	public void setDrawTooltip(boolean drawTooltip) {
		this.drawTooltip = drawTooltip;
	}

	public void setListener(IValueChangedListener<PaletteEntry> listener) {
		this.listener = listener;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		Iterator<PaletteEntry> it = palette.iterator();

		final int bx = x + offsetX;
		final int by = y + offsetY;

		int ry = by;

		OUTER: for (int column = 0; column < columnCount; column++) {
			final int ny = ry + areaSize;
			int rx = bx;

			for (int row = 0; row < rowSize; row++) {
				if (!it.hasNext()) break OUTER;
				final PaletteEntry entry = it.next();

				final int nx = rx + areaSize;
				drawRect(rx, ry, nx, ny, 0xFF000000 | entry.rgb);
				rx = nx;
			}

			ry = ny;
		}

	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (drawTooltip && isMouseOver(mouseX, mouseY)) {
			final PaletteEntry entry = findEntry(mouseX - x, mouseY - y);
			if (entry != null) drawHoveringText(entry.name, offsetX + mouseX, offsetY + mouseY, minecraft.fontRenderer);
		}
	}

	@Override
	public void mouseDown(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);

		if (listener != null) {
			final PaletteEntry entry = findEntry(mouseX, mouseY);
			if (entry != null) listener.valueChanged(entry);
		}
	}

	private PaletteEntry findEntry(int mouseX, int mouseY) {
		final int row = mouseX / areaSize;
		final int column = mouseY / areaSize;

		if (row < rowSize && column < columnCount) {
			final int index = column * rowSize + row;
			if (index >= 0 && index < palette.size()) return palette.get(index);
		}

		return null;
	}

}
