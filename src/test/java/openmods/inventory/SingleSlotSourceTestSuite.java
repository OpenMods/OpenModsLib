package openmods.inventory;

import static openmods.inventory.Utils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import net.minecraft.item.ItemStack;
import openmods.inventory.transfer.sources.IItemStackSource;
import openmods.inventory.transfer.sources.SingleSlotSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SingleSlotSourceTestSuite {

	public interface SinkMethods {
		public ItemStack getStack();

		public void setStack(ItemStack stack);

		public boolean canExtract(ItemStack stack);

		public void markDirty();
	}

	public @Mock SinkMethods mockedMethods;

	public IItemStackSource source;

	private void verifyNoInventoryChanges() {
		verify(mockedMethods, never()).setStack(any(ItemStack.class));
		verify(mockedMethods, never()).markDirty();
	}

	private void testCommitWithoutChanges() {
		assertFalse(source.hasChanges());
		assertFalse(source.commit());
		assertFalse(source.hasChanges());
	}

	private void testCommitWithChanges() {
		assertTrue(source.hasChanges());
		assertTrue(source.commit());
		assertFalse(source.hasChanges());
	}

	private void verifyCommitChanges() {
		verifyNoInventoryChanges();
		testCommitWithChanges();
		verify(mockedMethods).markDirty();
	}

	private void setStackInSlot(ItemStack stack) {
		when(mockedMethods.getStack()).thenReturn(stack);
	}

	private void setVerificationResult(boolean result) {
		when(mockedMethods.canExtract(any(ItemStack.class))).thenReturn(result);
	}

	private static void testItemStack(final ItemStack expected, ItemStack actual, final int amount) {
		assertNotNull(actual);
		assertEquals(amount, actual.stackSize);
		assertTrue(expected.isItemEqual(actual));
	}

	@Before
	public void setup() {
		source = new SingleSlotSource() {

			@Override
			protected void setStack(ItemStack stack) {
				mockedMethods.setStack(stack);
			}

			@Override
			protected void markDirty() {
				mockedMethods.markDirty();
			}

			@Override
			protected ItemStack getStack() {
				return mockedMethods.getStack();
			}

			@Override
			protected boolean canExtract(ItemStack stack) {
				return mockedMethods.canExtract(stack != null ? stack.copy() : null);
			}
		};
	}

	@Test
	public void testPullFromEmptyInventory() {
		assertNull(source.pull(10));
		assertEquals(0, source.totalExtracted());
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPullAllFromEmptyInventory() {
		assertNull(source.pullAll());
		assertEquals(0, source.totalExtracted());
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPullFromRestrictingInventory() {
		setStackInSlot(new ItemStack(ITEM_A, 15));
		setVerificationResult(false);

		assertNull(source.pull(10));
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 10)));
		assertEquals(0, source.totalExtracted());
		testCommitWithoutChanges();

		verifyNoInventoryChanges();
	}

	@Test
	public void testPullAllFromRestrictingInventory() {
		setStackInSlot(new ItemStack(ITEM_A, 15));
		setVerificationResult(false);

		assertNull(source.pullAll());
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));
		assertEquals(0, source.totalExtracted());
		testCommitWithoutChanges();

		verifyNoInventoryChanges();
	}

	@Test
	public void testPullAll() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pullAll();
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));
		assertNotNull(pulled);
		assertTrue(ItemStack.areItemStacksEqual(original, pulled));

		assertEquals(15, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).setStack(null);
	}

	@Test
	public void testPullsAfterPullAll() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pullAll();

		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));
		assertNotNull(pulled);
		assertTrue(ItemStack.areItemStacksEqual(original, pulled));

		assertNull(source.pull(10));
		assertNull(source.pullAll());

		assertEquals(15, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).setStack(null);
	}

	@Test
	public void testPartialPull() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pull(5);
		testItemStack(original, pulled, 5);

		assertEquals(5, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));
		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 10)));
	}

	@Test
	public void testOverpull() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pull(20);
		testItemStack(original, pulled, 15);

		assertEquals(15, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));
		verify(mockedMethods).setStack(null);
	}

	@Test
	public void testDoublePull() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pull(5);
		testItemStack(original, pulled, 5);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));

		pulled = source.pull(6);
		testItemStack(original, pulled, 6);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 11)));

		assertEquals(11, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 4)));
	}

	@Test
	public void testOverPullAfterPull() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pull(5);
		testItemStack(original, pulled, 5);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));

		pulled = source.pull(15);
		testItemStack(original, pulled, 10);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));

		assertEquals(15, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).setStack(null);
	}

	@Test
	public void testOverPullAllAfterPull() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);
		setVerificationResult(true);

		ItemStack pulled = source.pull(6);
		testItemStack(original, pulled, 6);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 6)));

		pulled = source.pullAll();
		testItemStack(original, pulled, 9);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));

		assertEquals(15, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).setStack(null);
	}

	@Test
	public void testPartialFail() {
		final ItemStack original = new ItemStack(ITEM_A, 15);
		final ItemStack contents = original.copy();
		setStackInSlot(contents);

		when(mockedMethods.canExtract(argThat(containsItem(ITEM_A, 5)))).thenReturn(true);
		when(mockedMethods.canExtract(argThat(containsItem(ITEM_A, 6)))).thenReturn(false);

		ItemStack pulled = source.pull(5);
		testItemStack(original, pulled, 5);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));

		pulled = source.pull(6);
		assertNull(pulled);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));

		assertEquals(5, source.totalExtracted());

		verifyCommitChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 10)));
	}
	
	@Test
	public void testPullAfterCommit() {
		setStackInSlot(new ItemStack(ITEM_A, 15));
		setVerificationResult(true);

		ItemStack pulled = source.pullAll();
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));
		assertNotNull(pulled);
		assertThat(pulled, containsItem(ITEM_A, 15));

		assertEquals(15, source.totalExtracted());

		verifyCommitChanges();
		verify(mockedMethods).setStack(null);
		
		pulled = source.pull(5);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));
		assertNotNull(pulled);
		assertThat(pulled, containsItem(ITEM_A, 5));
		assertEquals(5, source.totalExtracted());

		testCommitWithChanges();
		verify(mockedMethods, times(2)).markDirty();
		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 10)));
	}

	@Test
	public void testPullAfterAbort() {
		setStackInSlot(new ItemStack(ITEM_A, 15));
		setVerificationResult(true);

		ItemStack pulled = source.pullAll();
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 15)));
		assertNotNull(pulled);
		assertThat(pulled, containsItem(ITEM_A, 15));

		assertEquals(15, source.totalExtracted());

		source.abort();
		
		pulled = source.pull(5);
		verify(mockedMethods).canExtract(argThat(containsItem(ITEM_A, 5)));
		assertNotNull(pulled);
		assertThat(pulled, containsItem(ITEM_A, 5));
		assertEquals(5, source.totalExtracted());

		verifyCommitChanges();
		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 10)));
	}
}
