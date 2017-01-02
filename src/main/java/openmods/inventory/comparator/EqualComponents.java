package openmods.inventory.comparator;

import com.google.common.base.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EqualComponents {

	public interface IItemStackTester {
		public boolean isEqual(ItemStack left, ItemStack right);
	}

	public static final IItemStackTester ITEM_TESTER = new IItemStackTester() {
		@Override
		public boolean isEqual(ItemStack left, ItemStack right) {
			return left.getItem() == right.getItem();
		}
	};

	public static final IItemStackTester ITEM_ID_TESTER = new IItemStackTester() {
		@Override
		public boolean isEqual(ItemStack left, ItemStack right) {
			return Item.getIdFromItem(left.getItem()) == Item.getIdFromItem(right.getItem());
		}
	};

	public static final IItemStackTester DAMAGE_TESTER = new IItemStackTester() {
		@Override
		public boolean isEqual(ItemStack left, ItemStack right) {
			return left.getItemDamage() == right.getItemDamage();
		}
	};

	public static final IItemStackTester SIZE_TESTER = new IItemStackTester() {
		@Override
		public boolean isEqual(ItemStack left, ItemStack right) {
			return left.stackSize == right.stackSize;
		}
	};

	public static final IItemStackTester NBT_TESTER = new IItemStackTester() {
		@Override
		public boolean isEqual(ItemStack left, ItemStack right) {
			return Objects.equal(left.getTagCompound(), right.getTagCompound());
		}
	};
}
