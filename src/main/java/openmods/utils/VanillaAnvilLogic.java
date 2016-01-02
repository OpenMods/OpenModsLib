package openmods.utils;

import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;

public class VanillaAnvilLogic {

	private int materialCost;
	private int maximumCost;
	private ItemStack outputStack;

	public VanillaAnvilLogic(ItemStack inputStack, ItemStack modifierStack, boolean isCreativeMode, Optional<String> itemName) {
		this.materialCost = -1;
		this.outputStack = null;

		final String repairedItemName = itemName.orNull();
		// almost vanilla logic, with few renames and minor variable declaration cleanup
		this.maximumCost = 1;
		int l1 = 0;
		int i2 = 0;
		int j2 = 0;

		if (inputStack == null) {
			this.outputStack = null;
			this.maximumCost = 0;
			return;
		}

		ItemStack itemstack1 = inputStack.copy();

		Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
		boolean flag = false;
		i2 = i2 + inputStack.getRepairCost() + (modifierStack == null? 0 : modifierStack.getRepairCost());
		this.materialCost = 0;

		if (modifierStack != null) {
			if (!onAnvilChange(inputStack, modifierStack, repairedItemName, i2)) return;
			flag = modifierStack.getItem() == Items.enchanted_book && Items.enchanted_book.getEnchantments(modifierStack).tagCount() > 0;

			if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(inputStack, modifierStack)) {
				int j4 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

				if (j4 <= 0) {
					this.outputStack = null;
					this.maximumCost = 0;
					return;
				}

				int l4;

				for (l4 = 0; j4 > 0 && l4 < modifierStack.stackSize; ++l4) {
					int j5 = itemstack1.getItemDamage() - j4;
					itemstack1.setItemDamage(j5);
					++l1;
					j4 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
				}

				this.materialCost = l4;
			} else {
				if (!flag && (itemstack1.getItem() != modifierStack.getItem() || !itemstack1.isItemStackDamageable())) { return; }

				if (itemstack1.isItemStackDamageable() && !flag) {
					int k2 = inputStack.getMaxDamage() - inputStack.getItemDamage();
					int l2 = modifierStack.getMaxDamage() - modifierStack.getItemDamage();
					int i3 = l2 + itemstack1.getMaxDamage() * 12 / 100;
					int j3 = k2 + i3;
					int k3 = itemstack1.getMaxDamage() - j3;

					if (k3 < 0) {
						k3 = 0;
					}

					if (k3 < itemstack1.getMetadata()) {
						itemstack1.setItemDamage(k3);
						l1 += 2;
					}
				}

				Map<Integer, Integer> map1 = EnchantmentHelper.getEnchantments(modifierStack);
				for (int i5 : map1.keySet()) {
					Enchantment enchantment = Enchantment.getEnchantmentById(i5);

					if (enchantment != null) {
						int k5 = map.containsKey(i5)? map.get(i5).intValue() : 0;
						int l3 = map1.get(i5).intValue();
						int i6;

						if (k5 == l3) {
							++l3;
							i6 = l3;
						} else {
							i6 = Math.max(l3, k5);
						}

						l3 = i6;
						boolean flag1 = enchantment.canApply(inputStack);

						if (isCreativeMode || inputStack.getItem() == Items.enchanted_book) {
							flag1 = true;
						}

						for (int i4 : map.keySet()) {
							Enchantment e2 = Enchantment.getEnchantmentById(i4);
							if (i4 != i5 && !(enchantment.canApplyTogether(e2) && e2.canApplyTogether(enchantment))) {// Forge BugFix: Let Both enchantments veto being together
								flag1 = false;
								++l1;
							}
						}

						if (flag1) {
							if (l3 > enchantment.getMaxLevel()) {
								l3 = enchantment.getMaxLevel();
							}

							map.put(Integer.valueOf(i5), Integer.valueOf(l3));
							int l5 = 0;

							switch (enchantment.getWeight()) {
								case 1:
									l5 = 8;
									break;
								case 2:
									l5 = 4;
								case 3:
								case 4:
								case 6:
								case 7:
								case 8:
								case 9:
								default:
									break;
								case 5:
									l5 = 2;
									break;
								case 10:
									l5 = 1;
							}

							if (flag) {
								l5 = Math.max(1, l5 / 2);
							}

							l1 += l5 * l3;
						}
					}
				}
			}
		}

		if (flag && !itemstack1.getItem().isBookEnchantable(itemstack1, modifierStack)) itemstack1 = null;

		if (itemstack1 != null) {
			if (StringUtils.isBlank(repairedItemName)) {
				if (inputStack.hasDisplayName()) {
					j2 = 1;
					l1 += j2;
					itemstack1.clearCustomName();
				}
			}
			else if (!repairedItemName.equals(inputStack.getDisplayName())) {
				j2 = 1;
				l1 += j2;
				itemstack1.setStackDisplayName(repairedItemName);
			}
		}

		this.maximumCost = i2 + l1;

		if (l1 <= 0) {
			itemstack1 = null;
		}

		if (j2 == l1 && j2 > 0 && this.maximumCost >= 40) {
			this.maximumCost = 39;
		}

		if (this.maximumCost >= 40 && !isCreativeMode) {
			itemstack1 = null;
		}

		if (itemstack1 != null) {
			int k4 = itemstack1.getRepairCost();

			if (modifierStack != null && k4 < modifierStack.getRepairCost()) {
				k4 = modifierStack.getRepairCost();
			}

			k4 = k4 * 2 + 1;
			itemstack1.setRepairCost(k4);
			EnchantmentHelper.setEnchantments(map, itemstack1);
		}

		outputStack = itemstack1;
	}

	private boolean onAnvilChange(ItemStack inputItem, ItemStack modifierItem, String itemName, int baseCost) {
		AnvilUpdateEvent e = new AnvilUpdateEvent(inputItem, modifierItem, itemName, baseCost);
		if (MinecraftForge.EVENT_BUS.post(e)) return false;
		if (e.output != null) {
			this.outputStack = e.output;
			this.maximumCost = e.cost;
			this.materialCost = e.materialCost;
			return false;
		}
		return true;
	}

	public int getModifierCost() {
		return materialCost;
	}

	public int getLevelCost() {
		return maximumCost;
	}

	public ItemStack getOutputStack() {
		return outputStack;
	}
}
