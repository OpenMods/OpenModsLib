package openmods.utils;

import org.junit.Assert;
import org.junit.Test;

public class EnchantmentTest {

	private static final int[] xpBarCap = new int[] {
			7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37,
			42, 47, 52, 57, 62, 67, 72, 77, 82, 87, 92, 97, 102, 107,
			112, 121, 130, 139, 148, 157, 166, 175, 184, 193 };

	private static final int[] xpForLevel = new int[] {
			0,
			7, 16, 27, 40, 55, 72, 91, 112, 135, 160, 187, 216, 247, 280, 315,
			352, 394, 441, 493, 550, 612, 679, 751, 828, 910, 997, 1089, 1186, 1288, 1395,
			1507, 1628, 1758, 1897, 2045, 2202, 2368, 2543, 2727, 2920
	};

	@Test
	public void testBarCap() {
		for (int level = 1; level <= xpBarCap.length; level++)
			Assert.assertEquals("Level " + level, xpBarCap[level - 1], EnchantmentUtils.xpBarCap(level - 1));
	}

	@Test
	public void testXpForLevel() {
		int totalXp = 0;

		Assert.assertEquals("Level 0", 0, EnchantmentUtils.getExperienceForLevel(0));

		for (int level = 1; level <= xpBarCap.length; level++) {
			totalXp += xpBarCap[level - 1];
			final int actualTotalXp = EnchantmentUtils.getExperienceForLevel(level);
			Assert.assertEquals("Level " + level, totalXp, actualTotalXp);
			Assert.assertEquals("Level " + level, xpForLevel[level], actualTotalXp);
		}
	}

	@Test
	public void testLevelForXp() {
		Assert.assertEquals(0, EnchantmentUtils.getLevelForExperience(0));
		Assert.assertEquals(0, EnchantmentUtils.getLevelForExperience(1));
		Assert.assertEquals(0, EnchantmentUtils.getLevelForExperience(6));

		Assert.assertEquals(1, EnchantmentUtils.getLevelForExperience(7));
		Assert.assertEquals(1, EnchantmentUtils.getLevelForExperience(8));
		Assert.assertEquals(1, EnchantmentUtils.getLevelForExperience(15));

		Assert.assertEquals(2, EnchantmentUtils.getLevelForExperience(16));
		Assert.assertEquals(2, EnchantmentUtils.getLevelForExperience(26));

		Assert.assertEquals(3, EnchantmentUtils.getLevelForExperience(27));

		Assert.assertEquals(7, EnchantmentUtils.getLevelForExperience(100));
		Assert.assertEquals(11, EnchantmentUtils.getLevelForExperience(200));
		Assert.assertEquals(14, EnchantmentUtils.getLevelForExperience(300));
		Assert.assertEquals(17, EnchantmentUtils.getLevelForExperience(400));
		Assert.assertEquals(19, EnchantmentUtils.getLevelForExperience(500));
	}

}
