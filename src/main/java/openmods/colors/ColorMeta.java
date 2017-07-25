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
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.text.WordUtils;

public enum ColorMeta implements IStringSerializable {
	BLACK("black", 0x1E1B1B, EnumDyeColor.BLACK),
	RED("red", 0xB3312C, EnumDyeColor.RED),
	GREEN("green", 0x3B511A, EnumDyeColor.GREEN),
	BROWN("brown", 0x51301A, EnumDyeColor.BROWN),
	BLUE("blue", 0x253192, EnumDyeColor.BLUE),
	PURPLE("purple", 0x7B2FBE, EnumDyeColor.PURPLE),
	CYAN("cyan", 0x287697, EnumDyeColor.CYAN),
	LIGHT_GRAY("lightGray", "silver", 0xABABAB, EnumDyeColor.SILVER),
	GRAY("gray", 0x434343, EnumDyeColor.GRAY),
	PINK("pink", 0xD88198, EnumDyeColor.PINK),
	LIME("lime", 0x41CD34, EnumDyeColor.LIME),
	YELLOW("yellow", 0xDECF2A, EnumDyeColor.YELLOW),
	LIGHT_BLUE("lightBlue", "light_blue", 0x6689D3, EnumDyeColor.LIGHT_BLUE),
	MAGENTA("magenta", 0xC354CD, EnumDyeColor.MAGENTA),
	ORANGE("orange", 0xEB8844, EnumDyeColor.ORANGE),
	WHITE("white", 0xF0F0F0, EnumDyeColor.WHITE);

	public final int rgb;
	public final int vanillaDyeId;
	public final int vanillaBlockId;
	public final EnumDyeColor vanillaEnum;
	public final int oreId;
	public final int bitmask;
	public final String oreName;
	public final String name;
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

	private ColorMeta(String name, int rgb, EnumDyeColor vanilla) {
		this(name, name, rgb, vanilla);
	}

	private ColorMeta(String name, String textureName, int rgb, EnumDyeColor vanilla) {
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
	private static final Map<EnumDyeColor, ColorMeta> COLORS_BY_VANILLA_ENUM = Maps.newEnumMap(EnumDyeColor.class);

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

	public static ColorMeta fromVanillaEnum(EnumDyeColor color) {
		return COLORS_BY_VANILLA_ENUM.get(color);
	}

	public static ColorMeta fromBlockMeta(int meta) {
		final EnumDyeColor vanilla = EnumDyeColor.byMetadata(meta);
		return fromVanillaEnum(vanilla);
	}

	public static ColorMeta fromDyeDamage(int damage) {
		final EnumDyeColor vanilla = EnumDyeColor.byDyeDamage(damage);
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