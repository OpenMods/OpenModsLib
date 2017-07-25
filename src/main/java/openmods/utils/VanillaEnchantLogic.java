package openmods.utils;

import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
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

	@Nonnull
	private ItemStack toEnchant = ItemStack.EMPTY;

	private Level level;

	private int xpLevels;

	public boolean setup(@Nonnull ItemStack stack, Level level, int power) {
		if (stack.isEmpty() || !stack.isItemEnchantable()) return false;

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

	@Nonnull
	public ItemStack enchant() {
		if (toEnchant.isEmpty())
			return ItemStack.EMPTY;

		ItemStack enchantedItem = toEnchant.copy();
		final boolean isBook = enchantedItem.getItem() == Items.BOOK;

		final List<EnchantmentData> enchantmentsToApply = getEnchantmentList(toEnchant, level, xpLevels);
		if (!enchantmentsToApply.isEmpty()) {
			if (isBook) {
				enchantedItem = new ItemStack(Items.ENCHANTED_BOOK);
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

	private List<EnchantmentData> getEnchantmentList(@Nonnull ItemStack stack, Level level, int xpLevels) {
		rand.setSeed(seed + level.ordinal());
		List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(rand, stack, xpLevels, false);

		if (stack.getItem() == Items.BOOK && list.size() > 1) {
			list.remove(this.rand.nextInt(list.size()));
		}

		return list;
	}

}
