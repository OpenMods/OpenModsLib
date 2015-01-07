package openmods.inventory.comparator;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;

public class ComparatorComponents {

	@SuppressWarnings("unchecked")
	private static <T> Ordering<T> arbitrary() {
		return (Ordering<T>)Ordering.arbitrary();
	}

	public static final Ordering<ItemStack> ITEM_COMPARATOR = arbitrary().onResultOf(new Function<ItemStack, Item>() {
		@Override
		public Item apply(ItemStack input) {
			return input.getItem();
		}
	}).nullsLast();

	public static final Ordering<ItemStack> ITEM_ID_COMPARATOR = Ordering.natural().onResultOf(new Function<ItemStack, Integer>() {
		@Override
		public Integer apply(ItemStack input) {
			return Item.getIdFromItem(input.getItem());
		}
	}).nullsLast();

	public static final Ordering<ItemStack> DAMAGE_COMPARATOR = Ordering.natural().onResultOf(new Function<ItemStack, Integer>() {
		@Override
		public Integer apply(ItemStack input) {
			return input.getItemDamage();
		}
	}).nullsLast();

	public static final Ordering<ItemStack> SIZE_COMPARATOR = Ordering.natural().onResultOf(new Function<ItemStack, Integer>() {
		@Override
		public Integer apply(ItemStack input) {
			return input.stackSize;
		}
	}).nullsLast();
}
