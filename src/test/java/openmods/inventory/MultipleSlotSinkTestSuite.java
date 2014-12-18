package openmods.inventory;

import static openmods.inventory.Utils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import net.minecraft.item.ItemStack;
import openmods.inventory.transfer.sinks.IItemStackSink;
import openmods.inventory.transfer.sinks.MultipleSlotSink;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class MultipleSlotSinkTestSuite {

	public interface Callback {
		public void call();
	}

	public @Mock Callback markDirty;

	public @Mock IItemStackSink sinkA;

	public @Mock IItemStackSink sinkB;

	public IItemStackSink sink;

	private static void configureSlotAccept(IItemStackSink sink, Matcher<ItemStack> contents, final int amount) {
		when(sink.accept(argThat(contents))).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				ItemStack stack = (ItemStack)args[0];
				args[0] = stack != null ? stack.copy() : null;
				return amount;
			}});
	}
	
	private void createSink(IItemStackSink... sinks) {
		sink = new MultipleSlotSink(ImmutableList.copyOf(sinks)) {
			@Override
			protected void markDirty() {
				markDirty.call();
			}
		};
	}

	private void verifyDirty() {
		verify(markDirty).call();
	}

	private void verifyNotDirty() {
		verify(markDirty, never()).call();
	}

	private void createDefaultSink() {
		createSink(sinkA, sinkB);
	}

	@Test
	public void testEmpty() {
		createSink();

		ItemStack input = new ItemStack(ITEM_A);
		assertEquals(0, sink.accept(input));
		assertFalse(sink.commit());

		verifyNotDirty();
	}

	@Test
	public void testNull() {
		createDefaultSink();

		assertEquals(0, sink.accept(null));
		assertFalse(sink.commit());

		verifyNotDirty();
	}

	@Test
	public void testEmptyStack() {
		createDefaultSink();

		assertEquals(0, sink.accept(new ItemStack(ITEM_A, 0)));
		assertFalse(sink.commit());

		verifyNotDirty();
	}

	private void testCommitWithChanges() {
		assertTrue(sink.commit());

		verify(sinkA).commit();
		verify(sinkB).commit();
		verifyDirty();
	}

	private void testCommitWithoutChanges() {
		assertFalse(sink.commit());

		verify(sinkA).commit();
		verify(sinkB).commit();
		verifyNotDirty();
	}

	@Test
	public void testPartialInsertIntoBothSinks() {
		final Matcher<ItemStack> firstSinkContents = containsItem(ITEM_A, 5);
		configureSlotAccept(sinkA, firstSinkContents, 2);

		final Matcher<ItemStack> secondSinkContents = containsItem(ITEM_A, 3);
		configureSlotAccept(sinkB, secondSinkContents, 1);

		createDefaultSink();
		ItemStack original = new ItemStack(ITEM_A, 5);
		ItemStack input = original.copy();

		assertEquals(3, sink.accept(input));
		assertTrue(ItemStack.areItemStacksEqual(original, input));

		verify(sinkA).accept(argThat(firstSinkContents));
		verify(sinkB).accept(argThat(secondSinkContents));
		verifyNotDirty();

		when(sinkA.commit()).thenReturn(true);
		when(sinkB.commit()).thenReturn(true);

		testCommitWithChanges();
	}

	@Test
	public void testInsertStopOnFirstSlot() {
		final Matcher<ItemStack> firstSinkContents = containsItem(ITEM_A, 5);
		configureSlotAccept(sinkA, firstSinkContents, 5);

		createDefaultSink();
		ItemStack original = new ItemStack(ITEM_A, 5);
		ItemStack input = original.copy();

		assertEquals(5, sink.accept(input));
		assertTrue(ItemStack.areItemStacksEqual(original, input));

		verify(sinkA).accept(argThat(firstSinkContents));
		verify(sinkB, never()).accept(any(ItemStack.class));
		verifyNotDirty();

		when(sinkA.commit()).thenReturn(true);
		when(sinkB.commit()).thenReturn(false);

		testCommitWithChanges();
	}

	@Test
	public void testInsertFirstSlotFull() {
		final Matcher<ItemStack> sinkContents = containsItem(ITEM_A, 5);
		
		configureSlotAccept(sinkA, sinkContents, 0);
		configureSlotAccept(sinkB, sinkContents, 5);

		createDefaultSink();
		ItemStack original = new ItemStack(ITEM_A, 5);
		ItemStack input = original.copy();

		assertEquals(5, sink.accept(input));
		assertTrue(ItemStack.areItemStacksEqual(original, input));
		verifyNotDirty();

		verify(sinkA).accept(argThat(sinkContents));
		verify(sinkB).accept(argThat(sinkContents));

		when(sinkA.commit()).thenReturn(false);
		when(sinkB.commit()).thenReturn(true);

		testCommitWithChanges();
	}

	@Test
	public void testInsertBothSlotsFull() {
		final Matcher<ItemStack> sinkContents = containsItem(ITEM_A, 5);
		configureSlotAccept(sinkA, sinkContents, 0);
		configureSlotAccept(sinkB, sinkContents, 0);

		createDefaultSink();
		ItemStack original = new ItemStack(ITEM_A, 5);
		ItemStack input = original.copy();

		assertEquals(0, sink.accept(input));
		assertTrue(ItemStack.areItemStacksEqual(original, input));
		verifyNotDirty();

		verify(sinkA).accept(argThat(sinkContents));
		verify(sinkB).accept(argThat(sinkContents));

		when(sinkA.commit()).thenReturn(false);
		when(sinkB.commit()).thenReturn(false);

		testCommitWithoutChanges();
	}

	@Test
	public void testAbort() {
		createDefaultSink();
		sink.abort();

		verify(sinkA).abort();
		verify(sinkB).abort();
	}

	@Test
	public void testChanged() {
		createDefaultSink();
		when(sinkB.hasChanges()).thenReturn(true);

		assertTrue(sink.hasChanges());
	}
}
