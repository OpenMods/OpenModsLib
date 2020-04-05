package openmods.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Bootstrap;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.GameData;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ItemMoverTest {

	static {
		GameData.init();
		Bootstrap.register();
	}

	private static class TestHandler extends InvWrapper {
		public TestHandler(ItemStack... items) {
			super(new GenericInventory(items.length));
			for (int i = 0; i < items.length; i++)
				setStackInSlot(i, items[i]);
		}

		public void assertContents(ItemStack... items) {
			Assert.assertEquals(items.length, getSlots());
			for (int i = 0; i < items.length; i++) {
				final ItemStack expected = items[i];
				final ItemStack actual = getStackInSlot(i);
				Assert.assertTrue("expected: " + expected + ", actual: " + actual, ItemStack.areItemStacksEqual(expected, actual));
			}
		}
	}

	private static TestHandler inv(ItemStack... items) {
		return new TestHandler(items);
	}

	private static Iterable<IItemHandler> single(IItemHandler handler) {
		return ImmutableList.of(handler);
	}

	private static Iterable<IItemHandler> multiple(IItemHandler... handlers) {
		return ImmutableList.copyOf(handlers);
	}

	private static final ItemStack NULL_STACK = ItemStack.EMPTY;

	@Test
	public void testSingleItemPull() {
		final TestHandler source = inv(Utils.itemA(5));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(5, ItemMover.pullToSlot(target, 0, 5, single(source)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(5));
	}

	@Test
	public void testSingleItemPartialPull() {
		final TestHandler source = inv(Utils.itemA(5));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(3, ItemMover.pullToSlot(target, 0, 3, single(source)));
		source.assertContents(Utils.itemA(2));
		target.assertContents(Utils.itemA(3));
	}

	@Test
	public void testSingleItemPullEmptySource() {
		final TestHandler source = inv(NULL_STACK);
		final TestHandler target = inv(Utils.itemA(2));

		Assert.assertEquals(0, ItemMover.pullToSlot(target, 0, 3, single(source)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(2));
	}

	@Test
	public void testSinglePullItemEmptySourceTarget() {
		final TestHandler source = inv(NULL_STACK);
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(0, ItemMover.pullToSlot(target, 0, 64, single(source)));
		source.assertContents(NULL_STACK);
		target.assertContents(NULL_STACK);
	}

	@Test
	public void testSingleItemPullWithMerge() {
		final TestHandler source = inv(Utils.itemA(5));
		final TestHandler target = inv(Utils.itemA(6));

		Assert.assertEquals(5, ItemMover.pullToSlot(target, 0, 64, single(source)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(11));
	}

	@Test
	public void testSingleItemPullWithMergeOverflow() {
		final TestHandler source = inv(Utils.itemA(60));
		final TestHandler target = inv(Utils.itemA(5), NULL_STACK);

		Assert.assertEquals(59, ItemMover.pullToSlot(target, 0, 64, single(source)));
		source.assertContents(Utils.itemA(1));
		target.assertContents(Utils.itemA(64), NULL_STACK);
	}

	@Test
	public void testSingleItemPullSplitSlots() {
		final TestHandler source = inv(Utils.itemA(5), Utils.itemA(6));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(10, ItemMover.pullToSlot(target, 0, 10, single(source)));
		source.assertContents(NULL_STACK, Utils.itemA(1));
		target.assertContents(Utils.itemA(10));
	}

	@Test
	public void testSingleItemPullSplitSlotsOverflow() {
		final TestHandler source = inv(Utils.itemA(32), Utils.itemA(32));
		final TestHandler target = inv(Utils.itemA(1));

		Assert.assertEquals(63, ItemMover.pullToSlot(target, 0, 128, single(source)));
		source.assertContents(NULL_STACK, Utils.itemA(1));
		target.assertContents(Utils.itemA(64));
	}

	@Test
	public void testItemPullWithIncompatibleItem() {
		final TestHandler source = inv(Utils.itemA(10), Utils.itemB(10));
		final TestHandler target = inv(Utils.itemB(1));

		Assert.assertEquals(10, ItemMover.pullToSlot(target, 0, 128, single(source)));
		source.assertContents(Utils.itemA(10), NULL_STACK);
		target.assertContents(Utils.itemB(11));
	}

	@Test
	public void testSingleItemPullMultipleInventories() {
		final TestHandler sourceA = inv(Utils.itemA(5), Utils.itemA(6));
		final TestHandler sourceB = inv(Utils.itemA(7), Utils.itemA(8));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(16, ItemMover.pullToSlot(target, 0, 16, multiple(sourceA, sourceB)));
		sourceA.assertContents(NULL_STACK, NULL_STACK);
		sourceB.assertContents(Utils.itemA(2), Utils.itemA(8));
		target.assertContents(Utils.itemA(16));
	}

	@Test
	public void testItemPullFirstItemFiltering() {
		final TestHandler source = inv(Utils.itemA(5), Utils.itemB(6), Utils.itemA(7));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(12, ItemMover.pullToSlot(target, 0, 64, single(source)));
		source.assertContents(NULL_STACK, Utils.itemB(6), NULL_STACK);
		target.assertContents(Utils.itemA(12));
	}

	@Test
	public void testItemPullMultipleInventories() {
		final TestHandler sourceA = inv(Utils.itemA(5), Utils.itemB(6));
		final TestHandler sourceB = inv(Utils.itemB(7), Utils.itemA(8));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(13, ItemMover.pullToSlot(target, 0, 64, multiple(sourceA, sourceB)));
		sourceA.assertContents(NULL_STACK, Utils.itemB(6));
		sourceB.assertContents(Utils.itemB(7), NULL_STACK);
		target.assertContents(Utils.itemA(13));
	}

	@Test
	public void testSingleItemPush() {
		final TestHandler source = inv(Utils.itemA(5));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(5, ItemMover.pushFromSlot(source, 0, 5, single(target)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(5));
	}

	@Test
	public void testSingleItemPushWithItemAlreadyPresent() {
		final TestHandler source = inv(Utils.itemA(5));
		final TestHandler target = inv(Utils.itemA(6));

		Assert.assertEquals(5, ItemMover.pushFromSlot(source, 0, 5, single(target)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(11));
	}

	@Test
	public void testSingleItemPushWithLeftovers() {
		final TestHandler source = inv(Utils.itemA(6));
		final TestHandler target = inv(NULL_STACK);

		Assert.assertEquals(5, ItemMover.pushFromSlot(source, 0, 5, single(target)));
		source.assertContents(Utils.itemA(1));
		target.assertContents(Utils.itemA(5));
	}

	@Test
	public void testSingleItemPushWithEmptySource() {
		final TestHandler source = inv(NULL_STACK);
		final TestHandler target = inv(Utils.itemA(6));

		Assert.assertEquals(0, ItemMover.pushFromSlot(source, 0, 5, single(target)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(6));
	}

	@Test
	public void testSingleItemPushWithPartiallyIncompatibleSource() {
		final TestHandler source = inv(Utils.itemA(5));
		final TestHandler target = inv(Utils.itemB(6), Utils.itemA(4));

		Assert.assertEquals(5, ItemMover.pushFromSlot(source, 0, 64, single(target)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemB(6), Utils.itemA(9));
	}

	@Test
	public void testSingleItemPushWithIncompatibleTarget() {
		final TestHandler source = inv(Utils.itemA(6));
		final TestHandler target = inv(Utils.itemB(6));

		Assert.assertEquals(0, ItemMover.pushFromSlot(source, 0, 64, single(target)));
		source.assertContents(Utils.itemA(6));
		target.assertContents(Utils.itemB(6));
	}

	@Test
	public void testSingleItemPushWithSplit() {
		final TestHandler source = inv(Utils.itemA(63));
		final TestHandler target = inv(Utils.itemA(6), NULL_STACK);

		Assert.assertEquals(63, ItemMover.pushFromSlot(source, 0, 64, single(target)));
		source.assertContents(NULL_STACK);
		target.assertContents(Utils.itemA(64), Utils.itemA(5));
	}

	@Test
	public void testItemPushMultipleInventories() {
		final TestHandler source = inv(Utils.itemA(64));
		final TestHandler targetA = inv(Utils.itemA(40), Utils.itemB(6));
		final TestHandler targetB = inv(Utils.itemB(7), NULL_STACK);

		Assert.assertEquals(64, ItemMover.pushFromSlot(source, 0, 64, multiple(targetA, targetB)));
		source.assertContents(NULL_STACK);
		targetA.assertContents(Utils.itemA(64), Utils.itemB(6));
		targetB.assertContents(Utils.itemB(7), Utils.itemA(40));
	}

	@Test
	public void testItemPushMultipleInventoriesOverflow() {
		final TestHandler source = inv(Utils.itemA(64));
		final TestHandler targetA = inv(Utils.itemA(40), Utils.itemB(6));
		final TestHandler targetB = inv(Utils.itemB(7), Utils.itemA(40));

		Assert.assertEquals(48, ItemMover.pushFromSlot(source, 0, 64, multiple(targetA, targetB)));
		source.assertContents(Utils.itemA(16));
		targetA.assertContents(Utils.itemA(64), Utils.itemB(6));
		targetB.assertContents(Utils.itemB(7), Utils.itemA(64));
	}

}
