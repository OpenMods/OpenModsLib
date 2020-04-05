package openmods.inventory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class Utils {

	public static final Item ITEM_A = new Item(new Item.Properties()).setRegistryName("itemA");

	public static final Item ITEM_B = new Item(new Item.Properties()).setRegistryName("itemB");

	public static final Item ITEM_C = new Item(new Item.Properties().maxStackSize(2)).setRegistryName("itemC");

	public static ItemStack itemA(int amount) {
		return new ItemStack(ITEM_A, amount);
	}

	public static ItemStack itemB(int amount) {
		return new ItemStack(ITEM_B, amount);
	}

	public static ItemStack itemC(int amount) {
		return new ItemStack(ITEM_C, amount);
	}

	static Matcher<ItemStack> containsItem(final Item item, final int size) {
		return new BaseMatcher<ItemStack>() {
			@Override
			public boolean matches(Object o) {
				if (o instanceof ItemStack) {
					ItemStack stack = (ItemStack)o;
					return stack.getItem() == item && stack.getCount() == size;
				}

				return false;
			}

			@Override
			public void describeTo(Description arg0) {
				arg0.appendText(String.format("%dx%s", size, item.getTranslationKey()));
			}
		};
	}

	static Matcher<ItemStack> containsItem(final Item item) {
		return new BaseMatcher<ItemStack>() {
			@Override
			public boolean matches(Object o) {
				if (o instanceof ItemStack) {
					ItemStack stack = (ItemStack)o;
					return stack.getItem() == item;
				}

				return false;
			}

			@Override
			public void describeTo(Description arg0) {
				arg0.appendText(String.format("%s", item.getTranslationKey()));
			}
		};
	}
}
