package openmods.utils;

import java.util.Iterator;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;

public class VanillaAnvilLogic {

	private int modifierCost;
	private int levelCost;
	private ItemStack outputStack;

	public VanillaAnvilLogic(ItemStack inputStack, ItemStack modifierStack) {
		this.levelCost = 0;
		this.modifierCost = -1;
		this.outputStack = null;

		// almost vanilla logic, with few renames and minor variable declaration cleanup
		int i = 0;
		int j = 0;
		int k;
		int l;
		int i1;
		int k1;
		int l1;

		if (inputStack == null) return;

		ItemStack inputStackCopy = inputStack.copy();
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> inputEnchantments = EnchantmentHelper.getEnchantments(inputStackCopy);
		boolean isEnchantedBook = false;
		int materialCost = inputStack.getRepairCost() + (modifierStack == null? 0 : modifierStack.getRepairCost());

		if (modifierStack != null) {
			if (!onAnvilChange(inputStack, modifierStack, materialCost)) return;
			isEnchantedBook = modifierStack.getItem() == Items.enchanted_book && Items.enchanted_book.func_92110_g(modifierStack).tagCount() > 0;

			if (inputStackCopy.isItemStackDamageable() && inputStackCopy.getItem().getIsRepairable(inputStack, modifierStack)) {
				k = Math.min(inputStackCopy.getItemDamageForDisplay(), inputStackCopy.getMaxDamage() / 4);

				if (k <= 0) return;

				for (l = 0; k > 0 && l < modifierStack.stackSize; ++l) {
					i1 = inputStackCopy.getItemDamageForDisplay() - k;
					inputStackCopy.setItemDamage(i1);
					i += Math.max(1, k / 100) + inputEnchantments.size();
					k = Math.min(inputStackCopy.getItemDamageForDisplay(), inputStackCopy.getMaxDamage() / 4);
				}

				this.modifierCost = l;
			} else {
				if (!isEnchantedBook && (inputStackCopy.getItem() != modifierStack.getItem() || !inputStackCopy.isItemStackDamageable())) return;

				if (inputStackCopy.isItemStackDamageable() && !isEnchantedBook) {
					k = inputStack.getMaxDamage() - inputStack.getItemDamageForDisplay();
					l = modifierStack.getMaxDamage() - modifierStack.getItemDamageForDisplay();
					i1 = l + inputStackCopy.getMaxDamage() * 12 / 100;
					int j1 = k + i1;
					k1 = inputStackCopy.getMaxDamage() - j1;

					if (k1 < 0) k1 = 0;

					if (k1 < inputStackCopy.getItemDamage()) {
						inputStackCopy.setItemDamage(k1);
						i += Math.max(1, i1 / 100);
					}
				}

				@SuppressWarnings("unchecked")
				Map<Integer, Integer> modifierEnchantments = EnchantmentHelper.getEnchantments(modifierStack);
				Iterator<Integer> iterator1 = modifierEnchantments.keySet().iterator();

				while (iterator1.hasNext()) {
					i1 = iterator1.next();
					Enchantment enchantment = Enchantment.enchantmentsList[i1];
					k1 = inputEnchantments.containsKey(i1)? inputEnchantments.get(i1) : 0;
					l1 = modifierEnchantments.get(i1);
					int i3;

					if (k1 == l1) {
						++l1;
						i3 = l1;
					} else {
						i3 = Math.max(l1, k1);
					}

					l1 = i3;
					int i2 = l1 - k1;
					boolean flag1 = enchantment.canApply(inputStack);

					if (inputStack.getItem() == Items.enchanted_book) flag1 = true;

					Iterator<Integer> iterator = inputEnchantments.keySet().iterator();

					while (iterator.hasNext()) {
						int j2 = iterator.next();

						if (j2 != i1 && !enchantment.canApplyTogether(Enchantment.enchantmentsList[j2])) {
							flag1 = false;
							i += i2;
						}
					}

					if (flag1) {
						if (l1 > enchantment.getMaxLevel()) l1 = enchantment.getMaxLevel();

						inputEnchantments.put(i1, l1);
						int l2 = 0;

						switch (enchantment.getWeight()) {
							case 1:
								l2 = 8;
								break;
							case 2:
								l2 = 4;
							case 3:
							case 4:
							case 6:
							case 7:
							case 8:
							case 9:
							default:
								break;
							case 5:
								l2 = 2;
								break;
							case 10:
								l2 = 1;
						}

						if (isEnchantedBook) {
							l2 = Math.max(1, l2 / 2);
						}

						i += l2 * i2;
					}
				}
			}
		}

		k = 0;

		for (Iterator<Integer> iterator1 = inputEnchantments.keySet().iterator(); iterator1.hasNext(); materialCost += k + k1 * l1) {
			i1 = iterator1.next();
			Enchantment enchantment = Enchantment.enchantmentsList[i1];
			k1 = inputEnchantments.get(i1);
			l1 = 0;
			++k;

			switch (enchantment.getWeight()) {
				case 1:
					l1 = 8;
					break;
				case 2:
					l1 = 4;
				case 3:
				case 4:
				case 6:
				case 7:
				case 8:
				case 9:
				default:
					break;
				case 5:
					l1 = 2;
					break;
				case 10:
					l1 = 1;
			}

			if (isEnchantedBook) l1 = Math.max(1, l1 / 2);
		}

		if (isEnchantedBook) materialCost = Math.max(1, materialCost / 2);

		if (isEnchantedBook && !inputStackCopy.getItem().isBookEnchantable(inputStackCopy, modifierStack)) inputStackCopy = null;

		this.levelCost = materialCost + i;

		if (i <= 0) inputStackCopy = null;

		if (j == i && j > 0 && this.levelCost >= 40) this.levelCost = 39;

		if (this.levelCost >= 40) inputStackCopy = null;

		if (inputStackCopy != null) {
			l = inputStackCopy.getRepairCost();

			if (modifierStack != null && l < modifierStack.getRepairCost()) l = modifierStack.getRepairCost();

			if (inputStackCopy.hasDisplayName()) l -= 9;

			if (l < 0) l = 0;

			l += 2;
			inputStackCopy.setRepairCost(l);
			EnchantmentHelper.setEnchantments(inputEnchantments, inputStackCopy);
		}

		outputStack = inputStackCopy;
	}

	private boolean onAnvilChange(ItemStack inputItem, ItemStack modifierItem, int baseCost) {
		AnvilUpdateEvent e = new AnvilUpdateEvent(inputItem, modifierItem, inputItem.getDisplayName(), baseCost);
		if (MinecraftForge.EVENT_BUS.post(e)) return false;
		if (e.output != null) {
			this.outputStack = e.output;
			this.levelCost = e.cost;
			this.modifierCost = e.materialCost;
			return false;
		}
		return true;
	}

	public int getModifierCost() {
		return modifierCost;
	}

	public int getLevelCost() {
		return levelCost;
	}

	public ItemStack getOutputStack() {
		return outputStack;
	}
}
