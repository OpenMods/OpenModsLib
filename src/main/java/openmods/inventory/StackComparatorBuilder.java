package openmods.inventory;

import java.util.List;

import net.minecraft.item.ItemStack;
import openmods.inventory.comparator.ComparatorComponents;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class StackComparatorBuilder {

	private List<Ordering<ItemStack>> result = Lists.newArrayList();

	private boolean usedItem;
	private boolean usedItemId;
	private boolean usedDamage;
	private boolean usedSize;

	public StackComparatorBuilder useItem() {
		Preconditions.checkState(!usedItem);
		usedItem = true;
		result.add(ComparatorComponents.ITEM_COMPARATOR);
		return this;
	}

	public StackComparatorBuilder useItemId() {
		Preconditions.checkState(!usedItemId);
		usedItemId = true;
		result.add(ComparatorComponents.ITEM_ID_COMPARATOR);
		return this;
	}

	public StackComparatorBuilder useDamage() {
		Preconditions.checkState(!usedDamage);
		usedDamage = true;
		result.add(ComparatorComponents.DAMAGE_COMPARATOR);
		return this;
	}

	public StackComparatorBuilder useSize() {
		Preconditions.checkState(!usedSize);
		usedSize = true;
		result.add(ComparatorComponents.SIZE_COMPARATOR);
		return this;
	}

	public Ordering<ItemStack> build() {
		return Ordering.compound(result);
	}

}
