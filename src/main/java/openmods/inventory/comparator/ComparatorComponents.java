package openmods.inventory.comparator;

import com.google.common.collect.Ordering;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ComparatorComponents {

	@SuppressWarnings("unchecked")
	private static <T> Ordering<T> arbitrary() {
		return (Ordering<T>)Ordering.arbitrary();
	}

	public static final Ordering<ItemStack> ITEM_COMPARATOR = arbitrary().onResultOf(ItemStack::getItem).nullsLast();

	public static final Ordering<ItemStack> ITEM_ID_COMPARATOR = Ordering.natural().onResultOf((ItemStack input) -> Item.getIdFromItem(input.getItem())).nullsLast();

	public static final Ordering<ItemStack> DAMAGE_COMPARATOR = Ordering.natural().onResultOf(ItemStack::getDamage).nullsLast();

	public static final Ordering<ItemStack> SIZE_COMPARATOR = Ordering.natural().onResultOf(ItemStack::getCount).nullsLast();
}
