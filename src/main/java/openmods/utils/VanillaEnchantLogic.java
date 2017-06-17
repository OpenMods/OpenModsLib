package openmods.utils;

import java.util.List;
import java.util.Random;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class VanillaEnchantLogic {

	public enum Level {
		L1, L2, L3
	}

	private final long seed;

	private final Random rand = new Random();

	public VanillaEnchantLogic(long seed) {
		this.seed = seed;
	}

	private ItemStack toEnchant;

	private Level level;

	private int xpLevels;

	public boolean setup(ItemStack stack, Level level, int power) {
		if (stack == null || !stack.isItemEnchantable()) return false;

		rand.setSeed(seed);
		this.toEnchant = stack.copy();
		this.level = level;
		this.xpLevels = EnchantmentHelper.calcItemStackEnchantability(rand, level.ordinal(), power, toEnchant);

		if (this.xpLevels <= level.ordinal() + 1) this.xpLevels = 0;
		return true;
	}

	public int getLevelCost() {
		return level.ordinal() + 1;
	}

	public int getLevelRequirement() {
		return xpLevels;
	}

	public int getLapisCost() {
		return level.ordinal() + 1;
	}

	@SuppressWarnings("deprecation")
	public ItemStack enchant() {
		final ItemStack enchantedItem = toEnchant.copy();
		final boolean isBook = enchantedItem.getItem() == Items.BOOK;

		final List<EnchantmentData> enchantmentsToApply = getEnchantmentList(toEnchant, level, xpLevels);
		if (enchantmentsToApply != null) {
			if (isBook) {
				enchantedItem.setItem(Items.ENCHANTED_BOOK);
			}

			for (EnchantmentData enchantment : enchantmentsToApply) {
				if (isBook) {
					Items.ENCHANTED_BOOK.addEnchantment(enchantedItem, enchantment);
				} else {
					enchantedItem.addEnchantment(enchantment.enchantmentobj, enchantment.enchantmentLevel);
				}
			}
		}

		return enchantedItem;
	}

	private List<EnchantmentData> getEnchantmentList(ItemStack stack, Level level, int xpLevels) {
		rand.setSeed(seed + level.ordinal());
		List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(rand, stack, xpLevels, false);

		if (stack.getItem() == Items.BOOK && list.size() > 1) {
			list.remove(this.rand.nextInt(list.size()));
		}

		return list;
	}

}
