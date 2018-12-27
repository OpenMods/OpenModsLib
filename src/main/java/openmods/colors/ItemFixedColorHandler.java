package openmods.colors;

import javax.annotation.Nonnull;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
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
	public int colorMultiplier(@Nonnull ItemStack stack, int tintIndex) {
		return tintIndex == 0? color : COLOR_WHITE;
	}
}