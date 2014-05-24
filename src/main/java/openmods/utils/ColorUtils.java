package openmods.utils;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.text.WordUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ColorUtils {

	public static final int BLACK = 0;
	public static final int RED = 1;
	public static final int GREEN = 2;
	public static final int BROWN = 3;
	public static final int BLUE = 4;
	public static final int PURPLE = 5;
	public static final int CYAN = 6;
	public static final int LIGHT_GRAY = 7;
	public static final int GRAY = 8;
	public static final int PINK = 9;
	public static final int LIME = 10;
	public static final int YELLOW = 11;
	public static final int LIGHT_BLUE = 12;
	public static final int MAGENTA = 13;
	public static final int ORANGE = 14;
	public static final int WHITE = 15;

	public static ItemStack createDyedWool(int color) {
		return createDyedBlock(Blocks.wool, color);
	}

	public static ItemStack createStainedClay(int color) {
		return createDyedBlock(Blocks.stained_hardened_clay, color);
	}

	public static ItemStack createDyedCarpet(int color) {
		return createDyedBlock(Blocks.carpet, color);
	}

	public static ItemStack createDyedBlock(Block block, int color) {
		// TODO: Fix
		// int blockColor = BlockColored.getBlockFromDye(color);
		// return new ItemStack(block, 1, blockColor);
		return null;
	}

	public static class ColorMeta {
		public final int rgb;
		public final int vanillaId;
		public final int oreId;
		public final int bitmask;
		public final String oreName;
		public final String name;

		public ColorMeta(int rgb, int vanillaId, int oreId, String name, String oreName) {
			this.rgb = rgb;
			this.vanillaId = vanillaId;
			this.oreId = oreId;
			this.oreName = oreName;
			this.name = name;
			this.bitmask = 1 << vanillaId;
		}
	}

	private static final List<ColorMeta> COLORS = Lists.newArrayList();
	private static final Map<String, ColorMeta> COLORS_BY_ORE_NAME = Maps.newHashMap();
	private static final Map<String, ColorMeta> COLORS_BY_NAME = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_ORE_ID = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_BITMASK = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_VANILLA = Maps.newHashMap();

	private static void addEntry(String name, int colorValue, int vanillaId) {
		String oreName = "dye" + WordUtils.capitalize(name);
		int oreId = OreDictionary.getOreID(oreName);
		name = name.toLowerCase();
		ColorMeta color = new ColorMeta(colorValue, vanillaId, oreId, name, oreName);
		COLORS.add(color);
		COLORS_BY_NAME.put(name, color);
		COLORS_BY_ORE_NAME.put(oreName, color);
		COLORS_BY_ORE_ID.put(oreId, color);
		COLORS_BY_BITMASK.put(color.bitmask, color);
		COLORS_BY_VANILLA.put(vanillaId, color);
	}

	static {
		addEntry("black", 0x1E1B1B, BLACK);
		addEntry("red", 0xB3312C, RED);
		addEntry("green", 0x3B511A, GREEN);
		addEntry("brown", 0x51301A, BROWN);
		addEntry("blue", 0x253192, BLUE);
		addEntry("purple", 0x7B2FBE, PURPLE);
		addEntry("cyan", 0x287697, CYAN);
		addEntry("lightGray", 0xABABAB, LIGHT_GRAY);
		addEntry("gray", 0x434343, GRAY);
		addEntry("pink", 0xD88198, PINK);
		addEntry("lime", 0x41CD34, LIME);
		addEntry("yellow", 0xDECF2A, YELLOW);
		addEntry("lightBlue", 0x6689D3, LIGHT_BLUE);
		addEntry("magenta", 0xC354CD, MAGENTA);
		addEntry("orange", 0xEB8844, ORANGE);
		addEntry("white", 0xF0F0F0, WHITE);
	}

	public static ColorMeta stackToColor(ItemStack stack) {
		int oreId = OreDictionary.getOreID(stack);
		if (oreId < 0) return null;
		return COLORS_BY_ORE_ID.get(oreId);
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

	public static ColorMeta vanillaToColor(int vanillaId) {
		return COLORS_BY_VANILLA.get(vanillaId);
	}

	public static Collection<ColorMeta> getAllColors() {
		return Collections.unmodifiableCollection(COLORS);
	}

	public static int bitmaskToVanilla(int color) {
		int high = Integer.numberOfLeadingZeros(color);
		int low = Integer.numberOfTrailingZeros(color);
		Preconditions.checkArgument(high == 31 - low && low <= 16, "Invalid color value: %sb", Integer.toBinaryString(color));
		return low;
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
