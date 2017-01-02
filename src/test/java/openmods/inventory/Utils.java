package openmods.inventory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class Utils {

	public static final Item ITEM_A = new Item() {
		{
			setUnlocalizedName("itemA");
		}
	};

	public static final Item ITEM_B = new Item() {
		{
			setUnlocalizedName("itemB");
		}
	};

	public static final Item ITEM_C = new Item() {
		{
			setUnlocalizedName("itemC");
			setMaxStackSize(2);
		}
	};

	static Matcher<ItemStack> containsItem(final Item item, final int size) {
		return new BaseMatcher<ItemStack>() {
			@Override
			public boolean matches(Object o) {
				if (o instanceof ItemStack) {
					ItemStack stack = (ItemStack)o;
					return stack.getItem() == item && stack.stackSize == size;
				}

				return false;
			}

			@Override
			public void describeTo(Description arg0) {
				arg0.appendText(String.format("%dx%s", size, item.getUnlocalizedName()));
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
				arg0.appendText(String.format("%s", item.getUnlocalizedName()));
			}
		};
	}
}
