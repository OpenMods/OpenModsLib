package openmods.inventory.comparator;

import com.google.common.base.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EqualComponents {

	@FunctionalInterface
	public interface IItemStackTester {
		boolean isEqual(ItemStack left, ItemStack right);
	}

	public static final IItemStackTester ITEM_TESTER = (left, right) -> left.getItem() == right.getItem();

	public static final IItemStackTester ITEM_ID_TESTER = (left, right) -> Item.getIdFromItem(left.getItem()) == Item.getIdFromItem(right.getItem());

	public static final IItemStackTester DAMAGE_TESTER = (left, right) -> left.getDamage() == right.getDamage();

	public static final IItemStackTester SIZE_TESTER = (left, right) -> left.getCount() == right.getCount();

	public static final IItemStackTester NBT_TESTER = (left, right) -> Objects.equal(left.getTag(), right.getTag());
}
