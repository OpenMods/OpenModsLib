package openmods.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;
import net.minecraftforge.common.model.animation.IJointClip;
import openmods.model.eval.EvaluatorFactory;
import openmods.model.eval.EvaluatorFactory.IClipProvider;
import openmods.model.eval.IVarExpander;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class EvalModelTest {

	private static final ImmutableMap<String, Float> NO_ARGS = ImmutableMap.<String, Float> of();

	private static class Tester {
		private final Map<String, Float> state = Maps.newHashMap();

		private Map<String, Float> result;

		public Tester put(String key, float value) {
			state.put(key, value);
			return this;
		}

		public Tester run(IVarExpander expander) {
			this.result = expander.expand(ImmutableMap.copyOf(state));
			return this;
		}

		public Tester run(EvaluatorFactory expander) {
			return run(expander.createExpander());
		}

		public Tester validate() {
			Assert.assertEquals(this.state, this.result);
			return this;
		}
	}

	private static Tester start() {
		return new Tester();
	}

	@Test
	public void testSingleFloatVarAdd() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 1.3");

		start().run(factory).put("a", 1.3f).validate();
	}

	@Test
	public void testSingleFloatVarArithmetics() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 1.3 / 2.5 + 3.2 - -4.4 * 56");
		factory.appendStatement("b := (1.4 + 4.5) * +5.4 + 2 ^ 3");

		start().run(factory)
				.put("a", 1.3f / 2.5f + 3.2f - -4.4f * 56f)
				.put("b", (1.4f + 4.5f) * 5.4f + 8)
				.validate();
	}

	@Test
	public void testSingleIntVarAdd() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 1234");

		start().run(factory).put("a", 1234).validate();
	}

	@Test
	public void testSingleHexFloatAdd() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 0x12.34");

		start().run(factory).put("a", 0x12 + 0x34 / 256f).validate();
	}

	@Test
	public void testVarOverride() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 4.4");

		start().put("a", 2).run(factory).put("a", 4.4f).validate();
	}

	@Test
	public void testDoubleVarOverride() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 4.4");
		factory.appendStatement("b := 9.3");

		start().put("a", 2).put("b", 3).put("c", 4)
				.run(factory)
				.put("a", 4.4f).put("b", 9.3f).validate();
	}

	private static void testConstExpr(String expr, float expected) {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("ans := " + expr);
		start().run(factory).put("ans", expected).validate();
	}

	private static void testSingleVarExpr(String expr, String var, float value, float expected) {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("ans := " + expr);
		start().put(var, value).run(factory).put("ans", expected).validate();
	}

	@Test
	public void testSingleFloatVarOptimizations() {
		final float a = 1.53f;
		testSingleVarExpr("1 * a", "a", a, a);
		testSingleVarExpr("1 * (a / 4)", "a", a, a / 4);

		testSingleVarExpr("0 * a", "a", a, 0);
		testSingleVarExpr("0 * (a / 4)", "a", a, 0);

		testSingleVarExpr("a * 1", "a", a, a);
		testSingleVarExpr("(a / 4) * 1", "a", a, a / 4);

		testSingleVarExpr("a * 0", "a", a, 0);
		testSingleVarExpr("(a / 4) * 0", "a", a, 0);

		testSingleVarExpr("0 + a", "a", a, a);
		testSingleVarExpr("0 + (a * 4)", "a", a, a * 4);

		testSingleVarExpr("a - 0", "a", a, a);
		testSingleVarExpr("(a / 6) - 0", "a", a, a / 6);

		testSingleVarExpr("2 ^ 2 ^ a", "a", a, (float)Math.pow(4, a));

		testSingleVarExpr("(a + 4) ^ 1", "a", a, a + 4);
		testSingleVarExpr("1 ^ (a + 4)", "a", a, 1);
	}

	@Test
	public void testSpecialValueNonOptimizations() {
		final float nan = Float.NaN;
		final float inf = Float.POSITIVE_INFINITY;
		final float zero = 0;
		final float nzero = 1 / Float.NEGATIVE_INFINITY;

		Assert.assertNotEquals(Float.floatToIntBits(nzero), Float.floatToIntBits(zero)); // sanity check

		testSingleVarExpr("1 * inf", "inf", inf, inf);
		testSingleVarExpr("1 * nan", "nan", nan, nan);

		testConstExpr("(0 / 0)", nan);

		testConstExpr("0 * (1 / 0)", nan);
		testSingleVarExpr("0 * inf", "inf", inf, nan);
		testConstExpr("0 * (0 / 0)", nan);
		testSingleVarExpr("0 * nan", "nan", nan, nan);

		testConstExpr("(1 / 0) * 0", nan);
		testSingleVarExpr("inf * 0", "inf", inf, nan);
		testConstExpr("(0 / 0) * 0", nan);
		testSingleVarExpr("nan * 0", "nan", nan, nan);

		testSingleVarExpr("inf * inf", "inf", inf, inf);

		testSingleVarExpr("zero / 1", "zero", zero, zero);
		testSingleVarExpr("1 / zero", "zero", zero, inf);
		testSingleVarExpr("zero / zero", "zero", zero, nan);

		testSingleVarExpr("zero / 0", "zero", zero, nan);
		testSingleVarExpr("nan / 0", "zero", zero, nan);

		testSingleVarExpr("0 / nan", "nan", nan, nan);
		testSingleVarExpr("0 / inf", "inf", inf, zero);

		testSingleVarExpr("1 / nzero", "nzero", nzero, -inf);

		testConstExpr("-1 / (1 / 0)", nzero);
		testConstExpr("1 / (-1 / 0)", nzero);
		testSingleVarExpr("0 / -inf", "inf", inf, nzero);
		testSingleVarExpr("-1 / inf", "inf", inf, nzero);
		testSingleVarExpr("1 * nzero", "nzero", nzero, nzero);

		testSingleVarExpr("nzero * nzero", "nzero", nzero, zero);
		testSingleVarExpr("nzero * 0", "nzero", nzero, nzero);
		testSingleVarExpr("0 * nzero", "nzero", nzero, nzero);

		testSingleVarExpr("0 - nzero", "nzero", nzero, nzero);
		testSingleVarExpr("0 + nzero", "nzero", nzero, nzero);

		testSingleVarExpr("inf / inf", "inf", inf, nan);
		testSingleVarExpr("nan / nan", "nan", nan, nan);

		testSingleVarExpr("1 - inf", "inf", inf, -inf);
		testSingleVarExpr("1 - nan", "nan", nan, nan);

		testSingleVarExpr("inf + inf", "inf", inf, inf);
		testSingleVarExpr("inf + -inf", "inf", inf, nan);
		testSingleVarExpr("-inf + inf", "inf", inf, nan);

		testSingleVarExpr("inf - inf", "inf", inf, nan);
		testSingleVarExpr("inf - -inf", "inf", inf, inf);

		testSingleVarExpr("1 ^ zero", "zero", zero, 1);
		testSingleVarExpr("zero ^ 1", "zero", zero, zero);

		testSingleVarExpr("inf ^ inf", "inf", inf, inf);

		testSingleVarExpr("nan ^ 0", "nan", nan, 1);
		testSingleVarExpr("inf ^ 0", "inf", inf, 1);

		testSingleVarExpr("0 ^ zero", "zero", zero, 1);
		testSingleVarExpr("zero ^ 0", "zero", zero, 1);
	}

	@Test
	public void testVarArithmeticsOverride() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 3.3 / 2");
		factory.appendStatement("b := a + 3.6");
		factory.appendStatement("a := 3.4 * 3");

		start().put("c", 4)
				.run(factory)
				.put("a", 3.4f * 3).put("b", 3.3f / 2 + 3.6f).validate();
	}

	@Test
	public void testVarArithmeticsSelfOverride() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("a := 2 * a");
		factory.appendStatement("a := a * 3");
		factory.appendStatement("a := a + a");

		start().put("a", 4)
				.run(factory)
				.put("a", 2 * ((2 * 4) * 3)).validate();
	}

	private static final IJoint DUMMY_JOINT = new IJoint() {

		@Override
		public Optional<? extends IJoint> getParent() {
			throw new AssertionError("unsupported operation");
		}

		@Override
		public TRSRTransformation getInvBindPose() {
			throw new AssertionError("unsupported operation");
		}
	};

	private static class TestClipProvider implements IClipProvider {
		private final Map<String, IClip> clips = Maps.newHashMap();

		public TestClipProvider put(String key, IClip clip) {
			clips.put(key, clip);
			return this;
		}

		@Override
		public Optional<? extends IClip> get(String name) {
			return Optional.fromNullable(clips.get(name));
		}
	}

	private static TestClipProvider clips(String key, IClip clip) {
		return new TestClipProvider().put(key, clip);
	}

	private static class ClipStub implements IClip {

		public final IJointClip jointClipMock = Mockito.mock(IJointClip.class);

		private final IJoint expectedJoint;

		public ClipStub() {
			this(DUMMY_JOINT);
		}

		public ClipStub(IJoint expectedJoint) {
			this.expectedJoint = expectedJoint;
		}

		@Override
		public IJointClip apply(IJoint joint) {
			Assert.assertEquals(expectedJoint, joint);
			return jointClipMock;
		}

		@Override
		public Iterable<Event> pastEvents(float lastPollTime, float time) {
			throw new AssertionError("unsupported operation");
		}
	}

	@Test
	public void testDirectApply() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("clip(param)");

		final ClipStub clipStub = new ClipStub();
		final IJointClip jointClipMock = clipStub.jointClipMock;

		final TRSRTransformation transform = new TRSRTransformation(EnumFacing.NORTH);
		Mockito.when(jointClipMock.apply(Matchers.anyFloat())).thenReturn(transform);

		final float param = 1.3f;
		final TRSRTransformation result = factory.createEvaluator(clips("clip", clipStub)).evaluate(DUMMY_JOINT, ImmutableMap.of("param", param));
		Assert.assertEquals(transform, result);

		Mockito.verify(jointClipMock).apply(param);
		Mockito.verifyNoMoreInteractions(jointClipMock);
	}

	@Test
	public void testVarApply() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("param := 1.4");
		factory.appendStatement("clip(param)");

		final ClipStub clipStub = new ClipStub();
		final IJointClip jointClipMock = clipStub.jointClipMock;

		final TRSRTransformation transform = new TRSRTransformation(EnumFacing.NORTH);
		Mockito.when(jointClipMock.apply(Matchers.anyFloat())).thenReturn(transform);

		final TRSRTransformation result = factory.createEvaluator(clips("clip", clipStub)).evaluate(DUMMY_JOINT, NO_ARGS);
		Assert.assertEquals(transform, result);

		Mockito.verify(jointClipMock).apply(1.4f);
		Mockito.verifyNoMoreInteractions(jointClipMock);
	}

	@Test
	public void testConstApply() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("clip(2.4 + 1/3)");

		final ClipStub clipStub = new ClipStub();
		final IJointClip jointClipMock = clipStub.jointClipMock;

		final TRSRTransformation transform = new TRSRTransformation(EnumFacing.NORTH);
		Mockito.when(jointClipMock.apply(Matchers.anyFloat())).thenReturn(transform);

		final TRSRTransformation result = factory.createEvaluator(clips("clip", clipStub)).evaluate(DUMMY_JOINT, NO_ARGS);
		Assert.assertEquals(transform, result);

		Mockito.verify(jointClipMock).apply(2.4f + 1f / 3f);
		Mockito.verifyNoMoreInteractions(jointClipMock);
	}

	@Test
	public void testArithmeticsVarApply() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("clip(2.4 / a + 1/(3 * b))");

		final ClipStub clipStub = new ClipStub();
		final IJointClip jointClipMock = clipStub.jointClipMock;

		final TRSRTransformation transform = new TRSRTransformation(EnumFacing.NORTH);
		Mockito.when(jointClipMock.apply(Matchers.anyFloat())).thenReturn(transform);

		final TRSRTransformation result = factory.createEvaluator(clips("clip", clipStub))
				.evaluate(DUMMY_JOINT, ImmutableMap.of("a", 5.1f, "b", -0.4f));
		Assert.assertEquals(transform, result);

		Mockito.verify(jointClipMock).apply(2.4f / 5.1f + 1f / (3f * -0.4f));
		Mockito.verifyNoMoreInteractions(jointClipMock);
	}

	@Test
	public void testDoubleApply() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("param1 := 1.4");
		factory.appendStatement("clip(param1)");
		factory.appendStatement("clip(param2)");

		final ClipStub clipStub = new ClipStub();
		final IJointClip jointClipMock = clipStub.jointClipMock;

		final TRSRTransformation transform1 = new TRSRTransformation(EnumFacing.NORTH);
		final TRSRTransformation transform2 = new TRSRTransformation(EnumFacing.WEST);
		Mockito.when(jointClipMock.apply(1.4f)).thenReturn(transform1);
		Mockito.when(jointClipMock.apply(2.1f)).thenReturn(transform2);

		final TRSRTransformation result = factory.createEvaluator(clips("clip", clipStub)).evaluate(DUMMY_JOINT, ImmutableMap.of("param2", 2.1f));
		Assert.assertEquals(transform1.compose(transform2), result);

		Mockito.verify(jointClipMock).apply(1.4f);
		Mockito.verify(jointClipMock).apply(2.1f);
		Mockito.verifyNoMoreInteractions(jointClipMock);
	}

	@Test
	public void testSeparateClipsApply() {
		EvaluatorFactory factory = new EvaluatorFactory();
		factory.appendStatement("param := 2.5");
		factory.appendStatement("clip1(param)");
		factory.appendStatement("clip2(param)");

		final ClipStub clipStub1 = new ClipStub();
		final IJointClip jointClipMock1 = clipStub1.jointClipMock;

		final ClipStub clipStub2 = new ClipStub();
		final IJointClip jointClipMock2 = clipStub2.jointClipMock;

		final TRSRTransformation transform1 = new TRSRTransformation(EnumFacing.EAST);
		final TRSRTransformation transform2 = new TRSRTransformation(EnumFacing.UP);
		Mockito.when(jointClipMock1.apply(Matchers.anyFloat())).thenReturn(transform1);
		Mockito.when(jointClipMock2.apply(Matchers.anyFloat())).thenReturn(transform2);

		final TRSRTransformation result = factory.createEvaluator(clips("clip1", clipStub1).put("clip2", clipStub2)).evaluate(DUMMY_JOINT, NO_ARGS);
		Assert.assertEquals(transform1.compose(transform2), result);

		Mockito.verify(jointClipMock1).apply(2.5f);
		Mockito.verifyNoMoreInteractions(jointClipMock1);

		Mockito.verify(jointClipMock2).apply(2.5f);
		Mockito.verifyNoMoreInteractions(jointClipMock2);
	}
}
