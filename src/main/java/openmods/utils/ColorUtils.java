package openmods.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.text.WordUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ColorUtils {
	public static enum ColorMeta {
		BLACK("black", 0x1E1B1B),
		RED("red", 0xB3312C),
		GREEN("green", 0x3B511A),
		BROWN("brown", 0x51301A),
		BLUE("blue", 0x253192),
		PURPLE("purple", 0x7B2FBE),
		CYAN("cyan", 0x287697),
		LIGHT_GRAY("lightGray", 0xABABAB),
		GRAY("gray", 0x434343),
		PINK("pink", 0xD88198),
		LIME("lime", 0x41CD34),
		YELLOW("yellow", 0xDECF2A),
		LIGHT_BLUE("lightBlue", 0x6689D3),
		MAGENTA("magenta", 0xC354CD),
		ORANGE("orange", 0xEB8844),
		WHITE("white", 0xF0F0F0);

		public final int rgb;
		public final int vanillaDyeId;
		public final int vanillaBlockId;
		public final int oreId;
		public final int bitmask;
		public final String oreName;
		public final String name;
		public final RGB rgbWrap;
		public final CYMK cymkWrap;

		public ItemStack createStack(Block block, int amount) {
			return new ItemStack(block, amount, vanillaBlockId);
		}

		public ItemStack createStack(Item item, int amount) {
			return new ItemStack(item, amount, vanillaDyeId);
		}

		private ColorMeta(String name, int rgb) {
			this.oreName = "dye" + WordUtils.capitalize(name);
			this.oreId = OreDictionary.getOreID(oreName);
			this.name = name.toLowerCase();
			this.rgb = rgb;

			final int index = ordinal();
			this.vanillaDyeId = index;
			this.vanillaBlockId = ~index & 15;
			this.bitmask = 1 << vanillaBlockId;

			this.rgbWrap = new RGB(rgb);
			this.cymkWrap = rgbWrap.toCYMK();
		}

		private static final ColorMeta[] COLORS = ColorMeta.values();
	}

	private static final Map<String, ColorMeta> COLORS_BY_ORE_NAME = Maps.newHashMap();
	private static final Map<String, ColorMeta> COLORS_BY_NAME = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_ORE_ID = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_BITMASK = Maps.newHashMap();

	static {
		for (ColorMeta color : ColorMeta.COLORS) {
			COLORS_BY_NAME.put(color.name, color);
			COLORS_BY_ORE_NAME.put(color.oreName, color);
			COLORS_BY_ORE_ID.put(color.oreId, color);
			COLORS_BY_BITMASK.put(color.bitmask, color);
		}
	}

	public static Set<ColorMeta> stackToColor(ItemStack stack) {
		Set<ColorMeta> result = Sets.newIdentityHashSet();
		for (int oreId : OreDictionary.getOreIDs(stack)) {
			ColorMeta meta = COLORS_BY_ORE_ID.get(oreId);
			if (meta != null) result.add(meta);
		}
		return result;
	}

	public static ColorMeta oreIdToColor(int oreId) {
		return COLORS_BY_ORE_ID.get(oreId);
	}

	public static ColorMeta oreNameToColor(String oreName) {
		return COLORS_BY_ORE_NAME.get(oreName);
	}

	public static ColorMeta nameToColor(String name) {
		return COLORS_BY_NAME.get(name.toLowerCase());
	}

	public static ColorMeta bitmaskToColor(int bitmask) {
		return COLORS_BY_BITMASK.get(bitmask);
	}

	public static ColorMeta vanillaBlockToColor(int vanillaId) {
		return ColorMeta.COLORS[~vanillaId & 0xF];
	}

	public static ColorMeta vanillaDyeToColor(int vanillaId) {
		return ColorMeta.COLORS[vanillaId & 0xF];
	}

	public static Collection<ColorMeta> getAllColors() {
		return ImmutableList.copyOf(ColorMeta.COLORS);
	}

	public static int bitmaskToVanilla(int color) {
		int high = Integer.numberOfLeadingZeros(color);
		int low = Integer.numberOfTrailingZeros(color);
		Preconditions.checkArgument(high == 31 - low && low <= 16, "Invalid color value: %sb", Integer.toBinaryString(color));
		return low;
	}

	public static ColorMeta findNearestColor(RGB target, int tolernace) {
		ColorMeta result = null;
		int distSq = Integer.MAX_VALUE;

		for (ColorMeta meta : ColorMeta.COLORS) {
			final int currentDistSq = meta.rgbWrap.distanceSq(target);
			if (currentDistSq < distSq) {
				result = meta;
				distSq = currentDistSq;
			}
		}

		return (distSq < 3 * tolernace * tolernace)? result : null;
	}

	public static class RGB {
		public int r;
		public int g;
		public int b;

		public RGB(float r, float g, float b) {
			this.r = ((int)(r * 255)) & 0xFF;
			this.g = ((int)(g * 255)) & 0xFF;
			this.b = ((int)(b * 255)) & 0xFF;
		}

		public RGB(int r, int g, int b) {
			this.r = r & 0xFF;
			this.g = g & 0xFF;
			this.b = b & 0xFF;
		}

		public RGB(int color) {
			this(((color & 0xFF0000) >> 16), ((color & 0x00FF00) >> 8), (color & 0x0000FF));
		}

		public RGB() {}

		public void setColor(int color) {
			r = (color & 0xFF0000) >> 16;
			g = (color & 0x00FF00) >> 8;
			b = color & 0x0000FF;
		}

		public int getColor() {
			return r << 16 | g << 8 | b;
		}

		public float getR() {
			return r / 255f;
		}

		public float getG() {
			return g / 255f;
		}

		public float getB() {
			return b / 255f;
		}

		public RGB interpolate(RGB other, double amount) {
			int iPolR = (int)(r * (1D - amount) + other.r * amount);
			int iPolG = (int)(g * (1D - amount) + other.g * amount);
			int iPolB = (int)(b * (1D - amount) + other.b * amount);
			return new RGB(iPolR, iPolG, iPolB);
		}

		public CYMK toCYMK() {
			float cyan = 1f - (r / 255f);
			float magenta = 1f - (g / 255f);
			float yellow = 1f - (b / 255f);
			float K = 1;
			if (cyan < K) {
				K = cyan;
			}
			if (magenta < K) {
				K = magenta;
			}
			if (yellow < K) {
				K = yellow;
			}
			if (K == 1) {
				cyan = 0;
				magenta = 0;
				yellow = 0;
			} else {
				cyan = (cyan - K) / (1f - K);
				magenta = (magenta - K) / (1f - K);
				yellow = (yellow - K) / (1f - K);
			}
			return new CYMK(cyan, yellow, magenta, K);
		}

		public int distanceSq(RGB other) {
			final int dR = this.r - other.r;
			final int dG = this.g - other.g;
			final int dB = this.b - other.b;
			return (dR * dR) + (dG * dG) + (dB * dB);
		}
	}

	public static class CYMK {
		private float cyan, yellow, magenta, key;

		public CYMK(float c, float y, float m, float k) {
			this.cyan = c;
			this.yellow = y;
			this.magenta = m;
			this.key = k;
		}

		public float getCyan() {
			return cyan;
		}

		public void setCyan(float cyan) {
			this.cyan = cyan;
		}

		public float getYellow() {
			return yellow;
		}

		public void setYellow(float yellow) {
			this.yellow = yellow;
		}

		public float getMagenta() {
			return magenta;
		}

		public void setMagenta(float magenta) {
			this.magenta = magenta;
		}

		public float getKey() {
			return key;
		}

		public void setKey(float key) {
			this.key = key;
		}

	}

}
