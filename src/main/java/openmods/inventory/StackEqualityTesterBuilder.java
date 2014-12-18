package openmods.inventory;

import java.util.List;

import net.minecraft.item.ItemStack;
import openmods.inventory.comparator.EqualComponents;
import openmods.inventory.comparator.EqualComponents.IItemStackTester;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class StackEqualityTesterBuilder {

	public interface IEqualityTester {
		public boolean isEqual(Object left, Object right);
	}

	private boolean usedItem;
	private boolean usedItemId;
	private boolean usedDamage;
	private boolean usedSize;
	private boolean usedNBT;

	private List<IItemStackTester> testers = Lists.newArrayList();

	public StackEqualityTesterBuilder useItem() {
		Preconditions.checkState(!usedItem);
		usedItem = true;
		testers.add(EqualComponents.ITEM_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useItemId() {
		Preconditions.checkState(!usedItemId);
		usedItemId = true;
		testers.add(EqualComponents.ITEM_ID_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useDamage() {
		Preconditions.checkState(!usedDamage);
		usedDamage = true;
		testers.add(EqualComponents.DAMAGE_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useSize() {
		Preconditions.checkState(!usedSize);
		usedSize = true;
		testers.add(EqualComponents.SIZE_TESTER);
		return this;
	}

	public StackEqualityTesterBuilder useNBT() {
		Preconditions.checkState(!usedNBT);
		usedNBT = true;
		testers.add(EqualComponents.NBT_TESTER);
		return this;
	}

	public IEqualityTester build() {
		final List<IItemStackTester> frozenTesters = ImmutableList.copyOf(testers);

		return new IEqualityTester() {
			@Override
			public boolean isEqual(Object left, Object right) {
				if (left == right) return true;
				if ((left instanceof ItemStack) && (right instanceof ItemStack)) {
					for (IItemStackTester tester : frozenTesters)
						if (!tester.isEqual((ItemStack)left, (ItemStack)right)) return false;

					return true;
				}

				return false;
			}
		};
	}

	public Predicate<ItemStack> buildPredicate(ItemStack template) {
		final ItemStack copy = template.copy();
		final IEqualityTester tester = build();

		return new Predicate<ItemStack>() {
			@Override
			public boolean apply(ItemStack input) {
				return tester.isEqual(copy, input);
			}
		};
	}

}
