package openmods.calc;

import openmods.calc.IWhitespaceSplitter;
import openmods.calc.WhitespaceSplitters;
import org.junit.Assert;
import org.junit.Test;

public class WhitespaceSplitterTest {

	@Test
	public void onEmptyString_expectImmediateFinish() {
		onEmptyString_expectImmediateFinish(WhitespaceSplitters.fromSplitArray());
		onEmptyString_expectImmediateFinish(WhitespaceSplitters.fromString(""));
	}

	private static void onEmptyString_expectImmediateFinish(IWhitespaceSplitter splitter) {
		Assert.assertTrue(splitter.isFinished());
	}

	@Test
	public void onOneElement_expectSinglePart() {

		onOneElement_expectSinglePart(WhitespaceSplitters.fromSplitArray("aaa"));
		onOneElement_expectSinglePart(WhitespaceSplitters.fromString("aaa"));

	}

	private static void onOneElement_expectSinglePart(IWhitespaceSplitter splitter) {
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("aaa", splitter.getNextPart());
		Assert.assertTrue(splitter.isFinished());
	}

	@Test
	public void onOneElement_expectTailEqualPart() {
		onOneElement_expectTailEqualPart(WhitespaceSplitters.fromSplitArray("aaa"));
		onOneElement_expectTailEqualPart(WhitespaceSplitters.fromString("aaa"));
	}

	private static void onOneElement_expectTailEqualPart(IWhitespaceSplitter splitter) {
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("aaa", splitter.getTail());
		Assert.assertTrue(splitter.isFinished());
	}

	@Test
	public void onTwoElements_expectTwoParts() {
		onTwoElements_expectTwoParts(WhitespaceSplitters.fromSplitArray("aaa", "bbb"));
		onTwoElements_expectTwoParts(WhitespaceSplitters.fromString("aaa bbb"));
	}

	private static void onTwoElements_expectTwoParts(IWhitespaceSplitter splitter) {
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("aaa", splitter.getNextPart());
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("bbb", splitter.getNextPart());
		Assert.assertTrue(splitter.isFinished());
	}

	@Test
	public void onTwoElements_expectTailAfterFirstPart() {
		onTwoElements_expectTailAfterFirstPart(WhitespaceSplitters.fromSplitArray("aaa", "bbb"));
		onTwoElements_expectTailAfterFirstPart(WhitespaceSplitters.fromString("aaa bbb"));
	}

	private static void onTwoElements_expectTailAfterFirstPart(IWhitespaceSplitter splitter) {
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("aaa", splitter.getNextPart());
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("bbb", splitter.getTail());
		Assert.assertTrue(splitter.isFinished());
	}

	@Test
	public void onLastElementEmpty_expectEmptyTail() {
		IWhitespaceSplitter splitter = WhitespaceSplitters.fromString("aaa  ");
		Assert.assertFalse(splitter.isFinished());
		Assert.assertEquals("aaa", splitter.getNextPart());
		Assert.assertTrue(splitter.isFinished());
	}
}
