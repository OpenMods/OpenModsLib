package openmods.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ColorUtils {

	public static class ColorMeta {
		public final int rgb;
		public final int vanillaId;
		public final int oreId;
		public final String oreName;

		public ColorMeta(int rgb, int vanillaId, int oreId, String oreName) {
			this.rgb = rgb;
			this.vanillaId = vanillaId;
			this.oreId = oreId;
			this.oreName = oreName;
		}
	}

	private static final List<ColorMeta> COLORS = Lists.newArrayList();
	private static final Map<String, ColorMeta> COLORS_BY_ORE_NAME = Maps.newHashMap();
	private static final Map<Integer, ColorMeta> COLORS_BY_ORE_ID = Maps.newHashMap();

	private static void addEntry(String oreName, int colorValue, int vanillaId) {
		int oreId = OreDictionary.getOreID(oreName);
		ColorMeta color = new ColorMeta(colorValue, vanillaId, oreId, oreName);
		COLORS.add(color);
		COLORS_BY_ORE_NAME.put(oreName, color);
		COLORS_BY_ORE_ID.put(oreId, color);
	}

	static {
		addEntry("dyeBlack", 0x1E1B1B, 0);
		addEntry("dyeRed", 0xB3312C, 1);
		addEntry("dyeGreen", 0x3B511A, 2);
		addEntry("dyeBrown", 0x51301A, 3);
		addEntry("dyeBlue", 0x253192, 4);
		addEntry("dyePurple", 0x7B2FBE, 5);
		addEntry("dyeCyan", 0x287697, 6);
		addEntry("dyeLightGray", 0xABABAB, 7);
		addEntry("dyeGray", 0x434343, 8);
		addEntry("dyePink", 0xD88198, 9);
		addEntry("dyeLime", 0x41CD34, 10);
		addEntry("dyeYellow", 0xDECF2A, 11);
		addEntry("dyeLightBlue", 0x6689D3, 12);
		addEntry("dyeMagenta", 0xC354CD, 13);
		addEntry("dyeOrange", 0xEB8844, 14);
		addEntry("dyeWhite", 0xF0F0F0, 15);
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

	public static Collection<ColorMeta> getAllColors() {
		return Collections.unmodifiableCollection(COLORS);
	}

	public static class RGB {
		private int r, g, b;

		public RGB(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public RGB(int color) {
			this(((color & 0xFF0000) >> 16), ((color & 0x00FF00) >> 8), (color & 0x0000FF));
		}

		public RGB() {
			this(0);
		}

		public int getR() {
			return r;
		}

		public void setR(int r) {
			this.r = r;
		}

		public int getG() {
			return g;
		}

		public void setG(int g) {
			this.g = g;
		}

		public int getB() {
			return b;
		}

		public void setB(int b) {
			this.b = b;
		}

		public void setColor(int r, int g, int b) {
			setR(r);
			setG(g);
			setB(b);
		}

		public void setColor(int color) {
			setColor(((color & 0xFF0000) >> 16), ((color & 0x00FF00) >> 8), (color & 0x0000FF));
		}

		public int getColor() {
			return r << 16 | g << 8 | b;
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
