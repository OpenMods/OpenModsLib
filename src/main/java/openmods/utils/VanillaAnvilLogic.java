package openmods.utils;

import com.google.common.base.Optional;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import org.apache.commons.lang3.StringUtils;

public class VanillaAnvilLogic {

	private int materialCost;
	private int maximumCost;

	@Nonnull
	private ItemStack outputStack = ItemStack.EMPTY;

	public VanillaAnvilLogic(@Nonnull ItemStack inputStack, @Nonnull ItemStack modifierStack, boolean isCreativeMode, Optional<String> itemName) {
		this.materialCost = -1;

		final String repairedItemName = itemName.orNull();

		// adapted/copied from ContainerRepair.updateRepairOutput
		this.maximumCost = 1;
		int i = 0;
		int j = 0;
		int k = 0;

		if (inputStack.isEmpty()) {
			outputStack = ItemStack.EMPTY;
			this.maximumCost = 0;
		} else {
			ItemStack itemstack1 = inputStack.copy();
			ItemStack itemstack2 = modifierStack;
			Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
			j = j + inputStack.getRepairCost() + (itemstack2.isEmpty()? 0 : itemstack2.getRepairCost());
			this.materialCost = 0;
			boolean flag = false;

			if (!itemstack2.isEmpty()) {
				if (!onAnvilChange(inputStack, itemstack2, repairedItemName, j)) return;
				flag = itemstack2.getItem() == Items.ENCHANTED_BOOK && !ItemEnchantedBook.getEnchantments(itemstack2).hasNoTags();

				if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(inputStack, itemstack2)) {
					int l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

					if (l2 <= 0) {
						outputStack = ItemStack.EMPTY;
						this.maximumCost = 0;
						return;
					}

					int i3;

					for (i3 = 0; l2 > 0 && i3 < itemstack2.getCount(); ++i3) {
						int j3 = itemstack1.getItemDamage() - l2;
						itemstack1.setItemDamage(j3);
						++i;
						l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
					}

					this.materialCost = i3;
				} else {
					if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
						outputStack = ItemStack.EMPTY;
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
					boolean flag2 = false;
					boolean flag3 = false;

					for (Enchantment enchantment1 : map1.keySet()) {
						if (enchantment1 != null) {
							int i2 = map.containsKey(enchantment1)? map.get(enchantment1).intValue() : 0;
							int j2 = map1.get(enchantment1).intValue();
							j2 = i2 == j2? j2 + 1 : Math.max(j2, i2);
							boolean flag1 = enchantment1.canApply(inputStack);

							if (isCreativeMode || inputStack.getItem() == Items.ENCHANTED_BOOK) {
								flag1 = true;
							}

							for (Enchantment enchantment : map.keySet()) {
								if (enchantment != enchantment1 && !enchantment1.isCompatibleWith(enchantment)) {
									flag1 = false;
									++i;
								}
							}

							if (!flag1) {
								flag3 = true;
							} else {
								flag2 = true;

								if (j2 > enchantment1.getMaxLevel()) {
									j2 = enchantment1.getMaxLevel();
								}

								map.put(enchantment1, Integer.valueOf(j2));
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

								i += k3 * j2;

								if (inputStack.getCount() > 1) {
									i = 40;
								}
							}
						}
					}

					if (flag3 && !flag2) {
						outputStack = ItemStack.EMPTY;
						this.maximumCost = 0;
						return;
					}
				}
			}

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
			if (flag && !itemstack1.getItem().isBookEnchantable(itemstack1, itemstack2)) itemstack1 = ItemStack.EMPTY;

			this.maximumCost = j + i;

			if (i <= 0) {
				itemstack1 = ItemStack.EMPTY;
			}

			if (k == i && k > 0 && this.maximumCost >= 40) {
				this.maximumCost = 39;
			}

			if (this.maximumCost >= 40 && !isCreativeMode) {
				itemstack1 = ItemStack.EMPTY;
			}

			if (!itemstack1.isEmpty()) {
				int k2 = itemstack1.getRepairCost();

				if (!itemstack2.isEmpty() && k2 < itemstack2.getRepairCost()) {
					k2 = itemstack2.getRepairCost();
				}

				if (k != i || k == 0) {
					k2 = k2 * 2 + 1;
				}

				itemstack1.setRepairCost(k2);
				EnchantmentHelper.setEnchantments(map, itemstack1);
			}

			outputStack = itemstack1;
		}
	}

	private boolean onAnvilChange(@Nonnull ItemStack inputItem, @Nonnull ItemStack modifierItem, String itemName, int baseCost) {
		AnvilUpdateEvent e = new AnvilUpdateEvent(inputItem, modifierItem, itemName, baseCost);
		if (MinecraftForge.EVENT_BUS.post(e)) return false;
		if (!e.getOutput().isEmpty()) {
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

	@Nonnull
	public ItemStack getOutputStack() {
		return outputStack;
	}
}
