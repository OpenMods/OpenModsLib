package openmods.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import openmods.utils.Stack.StackUnderflowException;
import org.junit.Assert;
import org.junit.Test;

public class StackTest {

	public Stack<Integer> stack = Stack.create();

	protected void checkStackEmpty() {
		Assert.assertTrue(stack.isEmpty());
		Assert.assertEquals(0, stack.size());
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
		Assert.assertTrue(Iterables.elementsEqual(substack, ImmutableList.of(testValue)));

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
		Assert.assertEquals(2, substack.size());
		Assert.assertTrue(Iterables.elementsEqual(substack, ImmutableList.of(2, 3)));
	}

	@Test
	public void testStackAsSubstackNonZeroLength() {
		stack.push(1);
		stack.push(2);
		stack.push(3);

		final Stack<Integer> substack = stack.substack(3);

		Assert.assertEquals(3, substack.size());
		Assert.assertTrue(Iterables.elementsEqual(stack, substack));
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
		Assert.assertTrue(Iterables.elementsEqual(stack, substack));
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
}
