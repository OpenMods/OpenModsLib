package openmods.utils;

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

		Assert.assertEquals(stack.peek(0), value2);
		Assert.assertEquals(stack.peek(1), value1);

		final Integer retValue2 = stack.pop();
		Assert.assertEquals(value2, retValue2);

		final Integer retValue1 = stack.pop();
		Assert.assertEquals(value1, retValue1);

		checkStackEmpty();
	}
}
