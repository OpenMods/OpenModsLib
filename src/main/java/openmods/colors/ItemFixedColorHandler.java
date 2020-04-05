package openmods.colors;

import javax.annotation.Nonnull;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;

public class ItemFixedColorHandler implements IItemColor {
	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private final int color;

	public ItemFixedColorHandler(int color) {
		this.color = color;
	}

	public ItemFixedColorHandler(final ColorMeta color) {
		this.color = color.rgb;
	}

	@Override
	public int getColor(@Nonnull ItemStack stack, int tintIndex) {
		return tintIndex == 0? color : COLOR_WHITE;
	}
}