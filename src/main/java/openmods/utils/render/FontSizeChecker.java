package openmods.utils.render;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.util.ChatAllowedCharacters;
import openmods.OpenMods;

public class FontSizeChecker {

	private static FontSizeChecker instance = null;

	private int[] charWidth = new int[256];

	public static FontSizeChecker getInstance() {
		if (instance == null) {
			instance = new FontSizeChecker("textures/font/ascii.png");
		}
		return instance;
	}

	private FontSizeChecker(String textureFile) {
		readFontTexture(textureFile);
	}

	private void readFontTexture(String par1Str) {
		BufferedImage bufferedimage;
		try {
			bufferedimage = ImageIO.read(OpenMods.class.getResourceAsStream(par1Str));
		} catch (IOException ioexception) {
			throw new RuntimeException(ioexception);
		}
		int i = bufferedimage.getWidth();
		int j = bufferedimage.getHeight();
		int[] aint = new int[i * j];
		bufferedimage.getRGB(0, 0, i, j, aint, 0, i);
		int k = 0;

		while (k < 256) {
			int l = k % 16;
			int i1 = k / 16;
			int j1 = 7;

			while (true) {
				if (j1 >= 0) {
					int k1 = l * 8 + j1;
					boolean flag = true;

					for (int l1 = 0; l1 < 8 && flag; ++l1) {
						int i2 = (i1 * 8 + l1) * i;
						int j2 = aint[k1 + i2] & 255;

						if (j2 > 0) {
							flag = false;
						}
					}

					if (flag) {
						--j1;
						continue;
					}
				}

				if (k == 32) {
					j1 = 2;
				}
				this.charWidth[k] = j1 + 2;
				++k;
				break;
			}
		}
	}

	public int getCharWidth(char par1) {
		if (par1 == 167) {
			return -1;
		} else if (par1 == 32) {
			return 4;
		} else {
			int i = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000".indexOf(par1);
			if (par1 > 0 && i != -1) {
                return this.charWidth[i];
            }
		}
		return 8;
	}

	public int getStringWidth(String par1Str) {
		if (par1Str == null) { return 0; }
		int i = 0;
		boolean flag = false;

		for (int j = 0; j < par1Str.length(); ++j) {
			char c0 = par1Str.charAt(j);

			int k = getCharWidth(c0);

			if (k < 0 && j < par1Str.length() - 1) {
				++j;
				c0 = par1Str.charAt(j);

				if (c0 != 108 && c0 != 76) {
					if (c0 == 114 || c0 == 82) {
						flag = false;
					}
				} else {
					flag = true;
				}

				k = 0;
			}

			i += k;

			if (flag) {
				++i;
			}
		}

		return i;
	}

	public int getStringHeight(String par1Str) {
		return (par1Str == null? 0 : 8);
	}
}