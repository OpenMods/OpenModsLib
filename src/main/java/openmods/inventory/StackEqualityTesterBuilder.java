package openmods.inventory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import net.minecraft.item.ItemStack;
import openmods.inventory.comparator.EqualComponents;
import openmods.inventory.comparator.EqualComponents.IItemStackTester;

public class StackEqualityTesterBuilder {

	@FunctionalInterface
	public interface IEqualityTester {
		public boolean isEqual(Object left, Object right);
	}

	private boolean usedItem;
	private boolean usedItemId;
	private boolean usedDamage;
	private boolean usedSize;
	private boolean usedNBT;

	private IItemStackTester tester = null;

	private void compose(IItemStackTester newTerm) {
		if (tester == null) {
			tester = newTerm;
		} else {
			final IItemStackTester current = tester;
			tester = (left, right) -> newTerm.isEqual(left, right) && current.isEqual(left, right);
		}
	}

	public StackEqualityTesterBuilder useItem() {
		Preconditions.checkState(!usedItem);
		usedItem = true;
		compose(EqualComponents.ITEM_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useItemId() {
		Preconditions.checkState(!usedItemId);
		usedItemId = true;
		compose(EqualComponents.ITEM_ID_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useDamage() {
		Preconditions.checkState(!usedDamage);
		usedDamage = true;
		compose(EqualComponents.DAMAGE_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useSize() {
		Preconditions.checkState(!usedSize);
		usedSize = true;
		compose(EqualComponents.SIZE_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useNBT() {
		Preconditions.checkState(!usedNBT);
		usedNBT = true;
		compose(EqualComponents.NBT_TESTER);
		return this;
	}

	public IEqualityTester build() {
		final IItemStackTester tester = this.tester != null? this.tester : (left, right) -> true;

		return (left, right) -> {
			if (left == right) return true;
			if ((left instanceof ItemStack) && (right instanceof ItemStack)) { return tester.isEqual((ItemStack)left, (ItemStack)right); }
			return false;
		};
	}

	public Predicate<ItemStack> buildPredicate(ItemStack template) {
		final ItemStack copy = template.copy();
		final IEqualityTester tester = build();
		return input -> tester.isEqual(copy, input);
	}

}
