package openmods.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import openmods.utils.Stack.StackUnderflowException;
import org.junit.Assert;
import org.junit.Test;

public class StackTest {

	public Stack<Integer> stack = Stack.create();

	private void checkStackEmpty() {
		Assert.assertTrue(stack.isEmpty());
		Assert.assertEquals(0, stack.size());
	}

	private static void assertValuesOnStack(Stack<Integer> stack, Integer... values) {
		Assert.assertEquals("length", values.length, stack.size());
		Assert.assertEquals("values", ImmutableList.copyOf(stack), ImmutableList.copyOf(values));
	}

	private static void assertEquals(final Stack<Integer> expected, Stack<Integer> actual) {
		Assert.assertEquals(expected.size(), actual.size());
		Assert.assertEquals(ImmutableList.copyOf(expected), ImmutableList.copyOf(actual));
	}

	@Test
	public void testInitialEmpty() {
		checkStackEmpty();
	}

	@Test
	public void testPushPopSingleValue() {
		final Integer value = 24323;
		stack.push(value);
		Assert.assertFalse(stack.isEmpty());
		Assert.assertEquals(1, stack.size());

		Assert.assertEquals(stack.peek(0), value);

		final Integer retValue = stack.pop();
		Assert.assertEquals(value, retValue);

		checkStackEmpty();
	}

	@Test
	public void testPushPopTwoValues() {
		final Integer value1 = 24323;
		stack.push(value1);

		final Integer value2 = 54354;
		stack.push(value2);

		Assert.assertEquals(2, stack.size());

		Assert.assertEquals(value2, stack.peek(0));
		Assert.assertEquals(value1, stack.peek(1));

		final Integer retValue2 = stack.pop();
		Assert.assertEquals(value2, retValue2);

		final Integer retValue1 = stack.pop();
		Assert.assertEquals(value1, retValue1);

		checkStackEmpty();
	}

	@Test(expected = StackUnderflowException.class)
	public void testPeekEmptyStack() {
		stack.peek(0);
	}

	@Test(expected = StackUnderflowException.class)
	public void testPeekUnderTopStack() {
		stack.push(15);
		stack.peek(1);
	}

	@Test
	public void zeroLengthSubstack() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		final Stack<Integer> substack = stack.substack(0);
		Assert.assertEquals(0, substack.size());
		Assert.assertTrue(substack.isEmpty());
		Assert.assertTrue(Iterables.isEmpty(substack));

		final Integer testValue = 32432;
		substack.push(testValue);

		Assert.assertEquals(1, substack.size());
		Assert.assertFalse(substack.isEmpty());

		Assert.assertEquals(4, stack.size());
		Assert.assertEquals(testValue, substack.peek(0));
		Assert.assertEquals(testValue, stack.peek(0));
		assertValuesOnStack(substack, testValue);

		Assert.assertEquals(testValue, stack.pop());

		Assert.assertEquals(0, substack.size());
		Assert.assertTrue(substack.isEmpty());
	}

	@Test
	public void singleElementSubstack() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		final Integer testValue = 32432;
		stack.push(testValue);

		final Stack<Integer> substack = stack.substack(1);

		Assert.assertEquals(1, substack.size());
		Assert.assertFalse(substack.isEmpty());
		Assert.assertTrue(Iterables.elementsEqual(substack, ImmutableList.of(testValue)));

		Assert.assertEquals(testValue, substack.peek(0));
		Assert.assertEquals(testValue, substack.pop());
		Assert.assertTrue(Iterables.isEmpty(substack));

		Assert.assertEquals(0, substack.size());
		Assert.assertTrue(substack.isEmpty());

		Assert.assertEquals(3, stack.size());
	}

	@Test
	public void twoElementSubstack() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		final Stack<Integer> substack = stack.substack(2);
		assertValuesOnStack(substack, 2, 3);
	}

	@Test
	public void testStackAsSubstackNonZeroLength() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		final Stack<Integer> substack = stack.substack(3);

		Assert.assertEquals(3, substack.size());
		assertEquals(stack, substack);
	}

	@Test
	public void testStackAsSubstackZeroLength() {
		final Stack<Integer> substack = stack.substack(0);

		Assert.assertEquals(0, substack.size());
		Assert.assertTrue(substack.isEmpty());

		substack.push(1);
		substack.push(2);
		substack.push(3);

		Assert.assertEquals(3, substack.size());
		Assert.assertEquals(3, stack.size());
		assertEquals(stack, substack);
	}

	@Test(expected = StackUnderflowException.class)
	public void zeroLengthSubstackUnderflow() {
		stack.substack(1);
	}

	@Test(expected = StackUnderflowException.class)
	public void nonZeroLengthSubstackUnderflow() {
		stack.push(1);
		stack.substack(2);
	}

	@Test
	public void twoMultipleSubstackOperations() {
		stack.push(1);
		stack.push(2);
		stack.push(3);
		stack.push(4);

		final Stack<Integer> substack = stack.substack(3);
		assertValuesOnStack(substack, 2, 3, 4);

		{
			final Stack<Integer> subsubstack = substack.substack(3);
			assertValuesOnStack(subsubstack, 2, 3, 4);
		}

		{
			final Stack<Integer> subsubstack = substack.substack(2);
			assertValuesOnStack(subsubstack, 3, 4);
		}

		{
			final Stack<Integer> subsubstack = substack.substack(1);
			assertValuesOnStack(subsubstack, 4);
		}

		{
			final Stack<Integer> subsubstack = substack.substack(0);
			assertValuesOnStack(subsubstack);
		}
	}

	@Test
	public void twoSubstackUnderflow() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		final Stack<Integer> substack = stack.substack(1);

		try {
			substack.substack(2);
			Assert.fail();
		} catch (StackUnderflowException e) {}
	}

	@Test
	public void testDropFromTop() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		Assert.assertEquals(Integer.valueOf(3), stack.drop(0));
		assertValuesOnStack(stack, 1, 2);
	}

	@Test
	public void testDropFromMiddle() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		Assert.assertEquals(Integer.valueOf(2), stack.drop(1));
		assertValuesOnStack(stack, 1, 3);
	}

	@Test
	public void testDropFromBottom() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		Assert.assertEquals(Integer.valueOf(1), stack.drop(2));
		assertValuesOnStack(stack, 2, 3);
	}

	@Test(expected = StackUnderflowException.class)
	public void testInvalidDrop() {
		stack.drop(0);
	}
}
