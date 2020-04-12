package openmods.colors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.common.Tags;

public enum ColorMeta implements IStringSerializable {
	BLACK("black", 0x1E1B1B, DyeColor.BLACK, Tags.Items.DYES_BLACK),
	RED("red", 0xB3312C, DyeColor.RED, Tags.Items.DYES_RED),
	GREEN("green", 0x3B511A, DyeColor.GREEN, Tags.Items.DYES_GREEN),
	BROWN("brown", 0x51301A, DyeColor.BROWN, Tags.Items.DYES_BROWN),
	BLUE("blue", 0x253192, DyeColor.BLUE, Tags.Items.DYES_BLUE),
	PURPLE("purple", 0x7B2FBE, DyeColor.PURPLE, Tags.Items.DYES_PURPLE),
	CYAN("cyan", 0x287697, DyeColor.CYAN, Tags.Items.DYES_CYAN),
	LIGHT_GRAY("light_gray", 0xABABAB, DyeColor.LIGHT_GRAY, Tags.Items.DYES_LIGHT_GRAY),
	GRAY("gray", 0x434343, DyeColor.GRAY, Tags.Items.DYES_GRAY),
	PINK("pink", 0xD88198, DyeColor.PINK, Tags.Items.DYES_PINK),
	LIME("lime", 0x41CD34, DyeColor.LIME, Tags.Items.DYES_LIME),
	YELLOW("yellow", 0xDECF2A, DyeColor.YELLOW, Tags.Items.DYES_YELLOW),
	LIGHT_BLUE("light_blue", 0x6689D3, DyeColor.LIGHT_BLUE, Tags.Items.DYES_LIGHT_BLUE),
	MAGENTA("magenta", 0xC354CD, DyeColor.MAGENTA, Tags.Items.DYES_MAGENTA),
	ORANGE("orange", 0xEB8844, DyeColor.ORANGE, Tags.Items.DYES_ORANGE),
	WHITE("white", 0xF0F0F0, DyeColor.WHITE, Tags.Items.DYES_WHITE);

	public final int rgb;
	public final DyeColor vanillaEnum;
	public final ITag<Item> tag;
	public final String id;
	public final String unlocalizedName;
	public final RGB rgbWrap;
	public final CYMK cymkWrap;

	ColorMeta(String id, int rgb, DyeColor vanilla, ITag<Item> tag) {
		this.id = id;
		this.tag = tag;
		this.unlocalizedName = "openmodslib.color." + id;
		this.rgb = rgb;
		this.vanillaEnum = vanilla;
		this.rgbWrap = new RGB(rgb);
		this.cymkWrap = rgbWrap.toCYMK();
	}

	static final ColorMeta[] VALUES = ColorMeta.values();

	private static final Map<String, ColorMeta> COLORS_BY_ID = Maps.newHashMap();
	private static final Map<DyeColor, ColorMeta> COLORS_BY_VANILLA_ENUM = Maps.newEnumMap(DyeColor.class);

	static {
		for (ColorMeta color : values()) {
			COLORS_BY_ID.put(color.id, color);
			COLORS_BY_VANILLA_ENUM.put(color.vanillaEnum, color);
		}
	}

	public static Set<ColorMeta> fromStack(@Nonnull ItemStack stack) {
		Set<ColorMeta> result = Sets.newIdentityHashSet();
		final Item item = stack.getItem();
		for (ColorMeta meta : VALUES) {
			if (meta.tag.contains(item)) {
				result.add(meta);
			}
		}
		return result;
	}

	public static ColorMeta fromId(String id) {
		return COLORS_BY_ID.get(id);
	}

	public static ColorMeta fromVanillaEnum(DyeColor color) {
		return COLORS_BY_VANILLA_ENUM.get(color);
	}

	public static Collection<ColorMeta> getAllColors() {
		return ImmutableList.copyOf(VALUES);
	}

	@Override
	public String getString() {
		return id;
	}
}