package openmods.inventory;

import openmods.inventory.StackEqualityTesterBuilder.IEqualityTester;

public class ItemStackComparators {

	public static final IEqualityTester ITEM = new StackEqualityTesterBuilder().useItem().build();

	public static final IEqualityTester ITEM_DMG = new StackEqualityTesterBuilder().useItem().useDamage().build();

	public static final IEqualityTester ITEM_DMG_NBT = new StackEqualityTesterBuilder().useItem().useDamage().useNBT().build();

	public static final IEqualityTester FULL = new StackEqualityTesterBuilder().useItem().useSize().useDamage().useNBT().build();
}
