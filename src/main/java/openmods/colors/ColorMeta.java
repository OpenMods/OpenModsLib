package openmods.colors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.text.WordUtils;

public enum ColorMeta implements IStringSerializable {
	BLACK("black", 0x1E1B1B, DyeColor.BLACK),
	RED("red", 0xB3312C, DyeColor.RED),
	GREEN("green", 0x3B511A, DyeColor.GREEN),
	BROWN("brown", 0x51301A, DyeColor.BROWN),
	BLUE("blue", 0x253192, DyeColor.BLUE),
	PURPLE("purple", 0x7B2FBE, DyeColor.PURPLE),
	CYAN("cyan", 0x287697, DyeColor.CYAN),
	LIGHT_GRAY("lightGray", "light_gray", "silver", 0xABABAB, DyeColor.SILVER),
	GRAY("gray", 0x434343, DyeColor.GRAY),
	PINK("pink", 0xD88198, DyeColor.PINK),
	LIME("lime", 0x41CD34, DyeColor.LIME),
	YELLOW("yellow", 0xDECF2A, DyeColor.YELLOW),
	LIGHT_BLUE("lightBlue", "light_blue", "light_blue", 0x6689D3, DyeColor.LIGHT_BLUE),
	MAGENTA("magenta", 0xC354CD, DyeColor.MAGENTA),
	ORANGE("orange", 0xEB8844, DyeColor.ORANGE),
	WHITE("white", 0xF0F0F0, DyeColor.WHITE);

	public final int rgb;
	public final int vanillaDyeId;
	public final int vanillaBlockId;
	public final DyeColor vanillaEnum;
	public final int oreId;
	public final int bitmask;
	public final String oreName;
	public final String name;
	public final String id;
	public final String unlocalizedName;
	public final String textureName;
	public final RGB rgbWrap;
	public final CYMK cymkWrap;

	@Nonnull
	public ItemStack createStack(Block block, int amount) {
		return new ItemStack(block, amount, vanillaBlockId);
	}

	@Nonnull
	public ItemStack createStack(Item item, int amount) {
		return new ItemStack(item, amount, vanillaDyeId);
	}

	ColorMeta(String name, int rgb, DyeColor vanilla) {
		this(name, name, name, rgb, vanilla);
	}

	ColorMeta(String name, String id, String textureName, int rgb, DyeColor vanilla) {
		this.id = id;
		this.oreName = "dye" + WordUtils.capitalize(name);
		this.oreId = OreDictionary.getOreID(oreName);
		this.textureName = textureName;
		this.name = name.toLowerCase(Locale.ENGLISH);
		this.unlocalizedName = "openmodslib.color." + name;
		this.rgb = rgb;

		this.vanillaEnum = vanilla;

		this.vanillaDyeId = vanilla.getDyeDamage();
		this.vanillaBlockId = vanilla.getMetadata();
		this.bitmask = 1 << vanillaBlockId;

		this.rgbWrap = new RGB(rgb);
		this.cymkWrap = rgbWrap.toCYMK();
	}

	static final ColorMeta[] VALUES = ColorMeta.values();

	private static final Map<String, ColorMeta> COLORS_BY_ORE_NAME = Maps.newHashMap();
	private static final Map<String, ColorMeta> COLORS_BY_NAME = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_ORE_ID = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_BITMASK = Maps.newHashMap();
	private static final Map<DyeColor, ColorMeta> COLORS_BY_VANILLA_ENUM = Maps.newEnumMap(DyeColor.class);

	static {
		for (ColorMeta color : values()) {
			COLORS_BY_NAME.put(color.name, color);
			COLORS_BY_ORE_NAME.put(color.oreName, color);
			COLORS_BY_ORE_ID.put(color.oreId, color);
			COLORS_BY_BITMASK.put(color.bitmask, color);
			COLORS_BY_VANILLA_ENUM.put(color.vanillaEnum, color);
		}
	}

	public static Set<ColorMeta> fromStack(@Nonnull ItemStack stack) {
		Set<ColorMeta> result = Sets.newIdentityHashSet();
		for (int oreId : OreDictionary.getOreIDs(stack)) {
			ColorMeta meta = COLORS_BY_ORE_ID.get(oreId);
			if (meta != null) result.add(meta);
		}
		return result;
	}

	public static ColorMeta fromOreId(int oreId) {
		return COLORS_BY_ORE_ID.get(oreId);
	}

	public static ColorMeta fromOreName(String oreName) {
		return COLORS_BY_ORE_NAME.get(oreName);
	}

	public static ColorMeta fromName(String name) {
		return COLORS_BY_NAME.get(name.toLowerCase(Locale.ENGLISH));
	}

	public static ColorMeta fromBitmask(int bitmask) {
		return COLORS_BY_BITMASK.get(bitmask);
	}

	public static ColorMeta fromVanillaEnum(DyeColor color) {
		return COLORS_BY_VANILLA_ENUM.get(color);
	}

	public static ColorMeta fromBlockMeta(int meta) {
		final DyeColor vanilla = DyeColor.byMetadata(meta);
		return fromVanillaEnum(vanilla);
	}

	public static ColorMeta fromDyeDamage(int damage) {
		final DyeColor vanilla = DyeColor.byDyeDamage(damage);
		return fromVanillaEnum(vanilla);
	}

	public static Collection<ColorMeta> getAllColors() {
		return ImmutableList.copyOf(VALUES);
	}

	@Override
	public String getName() {
		return name;
	}
}