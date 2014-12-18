package openmods.inventory;

import static openmods.inventory.Utils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import net.minecraft.item.ItemStack;
import openmods.inventory.transfer.sinks.IItemStackSink;
import openmods.inventory.transfer.sinks.SingleSlotSink;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SingleSlotSinkTestSuite {

	public interface SinkMethods {
		public int getSlotLimit();

		public ItemStack getStack();

		public void setStack(ItemStack stack);

		public boolean isValid(ItemStack stack);

		public void markDirty();
	}

	public @Mock SinkMethods mockedMethods;

	public IItemStackSink sink;

	private void verifyNoInventoryChanges() {
		verify(mockedMethods, never()).setStack(any(ItemStack.class));
		verify(mockedMethods, never()).markDirty();
	}

	private void setStackInSlot(ItemStack stack) {
		when(mockedMethods.getStack()).thenReturn(stack);
	}

	private void configureDefaultInventory() {
		setDefaultSlotSize();
		setAllItemsValid();
	}

	private void setAllItemsValid() {
		when(mockedMethods.isValid(any(ItemStack.class))).thenReturn(true);
	}

	private void setDefaultSlotSize() {
		when(mockedMethods.getSlotLimit()).thenReturn(16);
	}

	private void verifyInventoryModified() {
		verify(mockedMethods).markDirty();
	}

	private void testCommitWithChanges() {
		assertTrue(sink.hasChanges());
		assertTrue(sink.commit());
		assertFalse(sink.hasChanges());
	}

	private void testCommitWithoutChanges() {
		assertFalse(sink.hasChanges());
		assertFalse(sink.commit());
		assertFalse(sink.hasChanges());
	}

	@Before
	public void setup() {
		sink = new SingleSlotSink() {

			@Override
			protected void setStack(ItemStack stack) {
				mockedMethods.setStack(stack);
			}

			@Override
			protected void markDirty() {
				mockedMethods.markDirty();
			}

			@Override
			protected boolean isValid(ItemStack stack) {
				return mockedMethods.isValid(stack);
			}

			@Override
			protected ItemStack getStack() {
				return mockedMethods.getStack();
			}

			@Override
			protected int getSlotLimit() {
				return mockedMethods.getSlotLimit();
			}
		};
	}

	@Test
	public void testNoChanges() {
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingNullToNullSlot() {
		configureDefaultInventory();
		assertEquals(0, sink.accept(null));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingNullToEmptySlot() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_B, 0));
		assertEquals(0, sink.accept(null));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingNullToFilledSlot() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_B, 1));
		assertEquals(0, sink.accept(null));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingEmptyStackToEmptySlot() {
		configureDefaultInventory();
		assertEquals(0, sink.accept(new ItemStack(ITEM_A, 0)));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingToLimitedSlot() {
		setAllItemsValid();
		when(mockedMethods.getSlotLimit()).thenReturn(0);
		assertEquals(0, sink.accept(new ItemStack(ITEM_A, 5)));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingToInvalidSlot() {
		setDefaultSlotSize();
		when(mockedMethods.isValid(any(ItemStack.class))).thenReturn(false);
		assertEquals(0, sink.accept(new ItemStack(ITEM_A, 5)));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingToEmptySlot() {
		configureDefaultInventory();

		assertEquals(7, sink.accept(new ItemStack(ITEM_A, 7)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 7)));
		verify(mockedMethods).isValid(argThat(containsItem(ITEM_A, 7)));
		verifyInventoryModified();
	}

	@Test
	public void testAddingAboveLimit() {
		setAllItemsValid();
		when(mockedMethods.getSlotLimit()).thenReturn(3);

		assertEquals(3, sink.accept(new ItemStack(ITEM_A, 5)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 3)));
		verify(mockedMethods).isValid(argThat(containsItem(ITEM_A)));
		verifyInventoryModified();
	}

	@Test
	public void testItemAdding() {
		configureDefaultInventory();

		assertEquals(5, sink.accept(new ItemStack(ITEM_A, 5)));
		assertEquals(8, sink.accept(new ItemStack(ITEM_A, 8)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 13)));
		verify(mockedMethods, times(2)).isValid(argThat(containsItem(ITEM_A)));
		verifyInventoryModified();
	}

	@Test
	public void testAddingIncompatibleItem() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A));

		assertEquals(0, sink.accept(new ItemStack(ITEM_B)));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPushingToZeroSlot() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A, 0));

		assertEquals(2, sink.accept(new ItemStack(ITEM_A, 2)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 2)));
		verify(mockedMethods).isValid(argThat(containsItem(ITEM_A)));
		verifyInventoryModified();
	}

	@Test
	public void testAddingIncompatibleItemAfterAddingNewOne() {
		configureDefaultInventory();

		assertEquals(5, sink.accept(new ItemStack(ITEM_A, 5)));
		assertEquals(0, sink.accept(new ItemStack(ITEM_B, 8)));
		assertEquals(4, sink.accept(new ItemStack(ITEM_A, 4)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 9)));
		verify(mockedMethods, times(2)).isValid(argThat(containsItem(ITEM_A)));
		verifyInventoryModified();
	}

	@Test
	public void testAddingToOccupiedSlot() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A, 10));

		assertEquals(2, sink.accept(new ItemStack(ITEM_A, 2)));
		assertEquals(3, sink.accept(new ItemStack(ITEM_A, 3)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 15)));
		verify(mockedMethods, times(2)).isValid(argThat(containsItem(ITEM_A)));
		verifyInventoryModified();
	}

	@Test
	public void testPartiallyAddingToOccupiedSlotAboveLimit() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A, 10));

		assertEquals(6, sink.accept(new ItemStack(ITEM_A, 10)));
		assertEquals(0, sink.accept(new ItemStack(ITEM_A, 10)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 16)));
		verify(mockedMethods, times(2)).isValid(argThat(containsItem(ITEM_A)));
		verifyInventoryModified();
	}

	@Test
	public void testAddingToOccupiedSlotAboveLimitStack() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_C, 2));

		assertEquals(0, sink.accept(new ItemStack(ITEM_C, 1)));
		testCommitWithoutChanges();
		verifyNoInventoryChanges();
	}

	@Test
	public void testPartiallyAddingToOccupiedSlotLimitStack() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_C, 1));

		assertEquals(1, sink.accept(new ItemStack(ITEM_C, 2)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_C, 2)));
		verify(mockedMethods).isValid(argThat(containsItem(ITEM_C)));
		verifyInventoryModified();
	}

	@Test
	public void testResetAfterCommit() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A, 10));

		assertEquals(6, sink.accept(new ItemStack(ITEM_A, 6)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 16)));
		verify(mockedMethods).markDirty();

		assertEquals(6, sink.accept(new ItemStack(ITEM_A, 6)));

		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 16)));
		verify(mockedMethods).markDirty();

		testCommitWithChanges();

		verify(mockedMethods, times(2)).setStack(argThat(containsItem(ITEM_A, 16)));
		verify(mockedMethods, times(2)).markDirty();

		testCommitWithoutChanges();
	}

	@Test
	public void testAbort() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A, 10));

		sink.abort();

		assertEquals(6, sink.accept(new ItemStack(ITEM_A, 6)));
		sink.abort();

		verify(mockedMethods).isValid(argThat(containsItem(ITEM_A)));
		verifyNoInventoryChanges();
	}

	@Test
	public void testCommitAfterAbort() {
		configureDefaultInventory();
		setStackInSlot(new ItemStack(ITEM_A, 10));

		assertEquals(6, sink.accept(new ItemStack(ITEM_A, 6)));
		sink.abort();

		assertEquals(5, sink.accept(new ItemStack(ITEM_A, 5)));
		verifyNoInventoryChanges();

		testCommitWithChanges();

		verify(mockedMethods, times(2)).isValid(argThat(containsItem(ITEM_A)));
		verify(mockedMethods).setStack(argThat(containsItem(ITEM_A, 15)));
		verifyInventoryModified();
	}

	@Test
	public void testNoInputStackModicationsOnFilledSlot() {
		configureDefaultInventory();
		final ItemStack originalContents = new ItemStack(ITEM_A, 10);
		final ItemStack insertedContents = originalContents.copy();
		setStackInSlot(insertedContents);

		final ItemStack originalStackA = new ItemStack(ITEM_A, 3);
		final ItemStack insertedStackA = originalStackA.copy();
		assertEquals(3, sink.accept(insertedStackA));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackA, originalStackA));

		final ItemStack originalStackB = new ItemStack(ITEM_A, 4);
		final ItemStack insertedStackB = originalStackB.copy();
		assertEquals(3, sink.accept(insertedStackB));
		assertTrue(ItemStack.areItemStacksEqual(originalContents, insertedContents));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackA, originalStackA));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackB, originalStackB));

		testCommitWithChanges();
		assertTrue(ItemStack.areItemStacksEqual(originalContents, insertedContents));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackA, originalStackA));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackB, originalStackB));
	}

	@Test
	public void testNoInputStackModicationsOnEmptySlot() {
		configureDefaultInventory();

		final ItemStack originalStackA = new ItemStack(ITEM_A, 10);
		final ItemStack insertedStackA = originalStackA.copy();
		assertEquals(10, sink.accept(insertedStackA));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackA, originalStackA));

		final ItemStack originalStackB = new ItemStack(ITEM_A, 10);
		final ItemStack insertedStackB = originalStackB.copy();
		assertEquals(6, sink.accept(insertedStackB));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackA, originalStackA));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackB, originalStackB));

		testCommitWithChanges();
		assertTrue(ItemStack.areItemStacksEqual(insertedStackA, originalStackA));
		assertTrue(ItemStack.areItemStacksEqual(insertedStackB, originalStackB));
	}

}
