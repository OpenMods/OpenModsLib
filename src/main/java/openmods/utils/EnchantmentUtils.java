package openmods.utils;

import java.util.List;
import java.util.Random;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public class EnchantmentUtils {

	public static int calcEnchantability(ItemStack itemStack, int power, boolean max) {
		Item item = itemStack.getItem();
		int k = item.getItemEnchantability();
		return calcEnchantability(k, power, max);
	}

	public static int calcEnchantability(int enchantability, int power, boolean max) {
		if (enchantability <= 0) { return 0; }
		if (power > 15) {
			power = 15;
		}

		int l = (max? 7 : 0) + 1 + (power >> 1) + (max? power : 0);
		return max? Math.max(l, power * 2) : Math.max(l / 3, 1);
	}

	/**
	 * Be warned, minecraft doesn't update experienceTotal properly, so we have
	 * to do this.
	 *
	 * @param player
	 * @return
	 */
	public static int getPlayerXP(EntityPlayer player) {
		return (int)(EnchantmentUtils.getExperienceForLevel(player.experienceLevel) + (player.experience * player.xpBarCap()));
	}

	public static void addPlayerXP(EntityPlayer player, int amount) {
		int experience = getPlayerXP(player) + amount;
		player.experienceTotal = experience;
		player.experienceLevel = EnchantmentUtils.getLevelForExperience(experience);
		int expForLevel = EnchantmentUtils.getExperienceForLevel(player.experienceLevel);
		player.experience = (float)(experience - expForLevel) / (float)player.xpBarCap();
	}

	// Similar to ContainerEnchanter.enchantItem
	public static boolean enchantItem(ItemStack itemstack, int level, Random rand, boolean allowTresure) {
		if (itemstack == null) return false;

		List<EnchantmentData> enchantments = EnchantmentHelper.buildEnchantmentList(rand, itemstack, level, allowTresure);
		if (enchantments == null || enchantments.isEmpty()) return false;

		boolean isBook = itemstack.getItem() == Items.BOOK;

		if (isBook) {
			itemstack.setItem(Items.ENCHANTED_BOOK);

			final int count = enchantments.size();
			int ignored = count > 1? rand.nextInt(count) : -1;
			for (int i = 0; i < count; i++)
				if (i != ignored) Items.ENCHANTED_BOOK.addEnchantment(itemstack, enchantments.get(i));
		} else {
			for (EnchantmentData enchantment : enchantments)
				itemstack.addEnchantment(enchantment.enchantmentobj, enchantment.enchantmentLevel);
		}

		return true;
	}

	public static int xpBarCap(int level) {
		if (level >= 30)
			return 112 + (level - 30) * 9;

		if (level >= 15)
			return 37 + (level - 15) * 5;

		return 7 + level * 2;
	}

	private static int sum(int n, int a0, int d) {
		return n * (2 * a0 + (n - 1) * d) / 2;
	}

	public static int getExperienceForLevel(int level) {
		if (level == 0) return 0;
		if (level <= 15) return sum(level, 7, 2);
		if (level <= 30) return 315 + sum(level - 15, 37, 5);
		return 1395 + sum(level - 30, 112, 9);
	}

	public static int getXpToNextLevel(int level) {
		int levelXP = EnchantmentUtils.getLevelForExperience(level);
		int nextXP = EnchantmentUtils.getExperienceForLevel(level + 1);
		return nextXP - levelXP;
	}

	public static int getLevelForExperience(int targetXp) {
		int level = 0;
		while (true) {
			final int xpToNextLevel = xpBarCap(level);
			if (targetXp < xpToNextLevel) return level;
			level++;
			targetXp -= xpToNextLevel;
		}
	}

	public static float getPower(World worldObj, BlockPos pos) {
		float power = 0;

		for (int x = -1; x <= 1; ++x) {
			for (int z = -1; z <= 1; ++z) {
				final int innerX = pos.getX() + z;
				final int innerZ = pos.getZ() + x;
				final int outerX = pos.getX() + z * 2;
				final int outerZ = pos.getZ() + x * 2;
				final int middle = pos.getY();
				final int top = pos.getY() + 1;

				if ((x != 0 || z != 0)
						&& worldObj.isAirBlock(new BlockPos(innerX, middle, innerZ))
						&& worldObj.isAirBlock(new BlockPos(innerX, top, innerZ))) {

					power += ForgeHooks.getEnchantPower(worldObj, new BlockPos(outerX, middle, outerZ));
					power += ForgeHooks.getEnchantPower(worldObj, new BlockPos(outerX, top, outerZ));

					if (z != 0 && x != 0) {
						power += ForgeHooks.getEnchantPower(worldObj, new BlockPos(outerX, middle, innerZ));
						power += ForgeHooks.getEnchantPower(worldObj, new BlockPos(outerX, top, innerZ));
						power += ForgeHooks.getEnchantPower(worldObj, new BlockPos(innerX, middle, outerZ));
						power += ForgeHooks.getEnchantPower(worldObj, new BlockPos(innerX, top, outerZ));
					}
				}
			}
		}
		return power;
	}

	public static void addAllBooks(Enchantment enchantment, List<ItemStack> items) {
		for (int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); i++)
			items.add(Items.ENCHANTED_BOOK.getEnchantedItemStack(new EnchantmentData(enchantment, i)));
	}
}