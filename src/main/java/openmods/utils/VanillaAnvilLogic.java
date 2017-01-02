package openmods.utils;

import com.google.common.base.Optional;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import org.apache.commons.lang3.StringUtils;

public class VanillaAnvilLogic {

	private int materialCost;
	private int maximumCost;
	private ItemStack outputStack;

	public VanillaAnvilLogic(ItemStack inputStack, ItemStack modifierStack, boolean isCreativeMode, Optional<String> itemName) {
		this.materialCost = -1;
		this.outputStack = null;

		final String repairedItemName = itemName.orNull();

		// adapted/copied from ContainerRepair.updateRepairOutput
		this.maximumCost = 1;
		int i = 0;
		int j = 0;
		int k = 0;

		if (inputStack == null) {
			this.outputStack = null;
			this.maximumCost = 0;
		} else {
			ItemStack itemstack1 = inputStack.copy();
			ItemStack itemstack2 = modifierStack;
			Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
			j = j + inputStack.getRepairCost() + (itemstack2 == null? 0 : itemstack2.getRepairCost());
			this.materialCost = 0;
			boolean flag = false;

			if (itemstack2 != null) {
				if (!onAnvilChange(inputStack, itemstack2, repairedItemName, j)) return;
				flag = itemstack2.getItem() == Items.ENCHANTED_BOOK && !Items.ENCHANTED_BOOK.getEnchantments(itemstack2).hasNoTags();

				if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(inputStack, itemstack2)) {
					int j2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

					if (j2 <= 0) {
						this.outputStack = null;
						this.maximumCost = 0;
						return;
					}

					int k2;

					for (k2 = 0; j2 > 0 && k2 < itemstack2.stackSize; ++k2) {
						int l2 = itemstack1.getItemDamage() - j2;
						itemstack1.setItemDamage(l2);
						++i;
						j2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
					}

					this.materialCost = k2;
				} else {
					if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
						this.outputStack = null;
						this.maximumCost = 0;
						return;
					}

					if (itemstack1.isItemStackDamageable() && !flag) {
						int l = inputStack.getMaxDamage() - inputStack.getItemDamage();
						int i1 = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
						int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
						int k1 = l + j1;
						int l1 = itemstack1.getMaxDamage() - k1;

						if (l1 < 0) {
							l1 = 0;
						}

						if (l1 < itemstack1.getMetadata()) {
							itemstack1.setItemDamage(l1);
							i += 2;
						}
					}

					Map<Enchantment, Integer> map1 = EnchantmentHelper.getEnchantments(itemstack2);

					for (Enchantment enchantment1 : map1.keySet()) {
						if (enchantment1 != null) {
							int i3 = map.containsKey(enchantment1)? map.get(enchantment1).intValue() : 0;
							int j3 = map1.get(enchantment1).intValue();
							j3 = i3 == j3? j3 + 1 : Math.max(j3, i3);
							boolean flag1 = enchantment1.canApply(inputStack);

							if (isCreativeMode || inputStack.getItem() == Items.ENCHANTED_BOOK) {
								flag1 = true;
							}

							for (Enchantment enchantment : map.keySet()) {
								if (enchantment != enchantment1 && !(enchantment1.canApplyTogether(enchantment) && enchantment.canApplyTogether(enchantment1)))  // Forge BugFix: Let Both enchantments veto being together
								{
									flag1 = false;
									++i;
								}
							}

							if (flag1) {
								if (j3 > enchantment1.getMaxLevel()) {
									j3 = enchantment1.getMaxLevel();
								}

								map.put(enchantment1, Integer.valueOf(j3));
								int k3 = 0;

								switch (enchantment1.getRarity()) {
									case COMMON:
										k3 = 1;
										break;
									case UNCOMMON:
										k3 = 2;
										break;
									case RARE:
										k3 = 4;
										break;
									case VERY_RARE:
										k3 = 8;
								}

								if (flag) {
									k3 = Math.max(1, k3 / 2);
								}

								i += k3 * j3;
							}
						}
					}
				}
			}

			if (flag && !itemstack1.getItem().isBookEnchantable(itemstack1, itemstack2)) itemstack1 = null;

			if (itemstack1 != null) {
				if (StringUtils.isBlank(repairedItemName)) {
					if (inputStack.hasDisplayName()) {
						k = 1;
						i += k;
						itemstack1.clearCustomName();
					}
				} else if (!repairedItemName.equals(inputStack.getDisplayName())) {
					k = 1;
					i += k;
					itemstack1.setStackDisplayName(repairedItemName);
				}
			}

			this.maximumCost = j + i;

			if (i <= 0) {
				itemstack1 = null;
			}

			if (k == i && k > 0 && this.maximumCost >= 40) {
				this.maximumCost = 39;
			}

			if (this.maximumCost >= 40 && !isCreativeMode) {
				itemstack1 = null;
			}

			if (itemstack1 != null) {
				int i2 = itemstack1.getRepairCost();

				if (itemstack2 != null && i2 < itemstack2.getRepairCost()) {
					i2 = itemstack2.getRepairCost();
				}

				if (k != i || k == 0) {
					i2 = i2 * 2 + 1;
				}

				itemstack1.setRepairCost(i2);
				EnchantmentHelper.setEnchantments(map, itemstack1);
			}

			this.outputStack = itemstack1;
		}
	}

	private boolean onAnvilChange(ItemStack inputItem, ItemStack modifierItem, String itemName, int baseCost) {
		AnvilUpdateEvent e = new AnvilUpdateEvent(inputItem, modifierItem, itemName, baseCost);
		if (MinecraftForge.EVENT_BUS.post(e)) return false;
		if (e.getOutput() != null) {
			this.outputStack = e.getOutput();
			this.maximumCost = e.getCost();
			this.materialCost = e.getMaterialCost();
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
