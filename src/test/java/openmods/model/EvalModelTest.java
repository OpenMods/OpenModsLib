package openmods.model;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
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

	private static class AccessCountingMap extends ForwardingMap<String, Float> {
		private final Map<String, Float> parent;

		private final Multiset<String> accessCounters;

		public AccessCountingMap(Map<String, Float> parent, Multiset<String> accessCounters) {
			this.parent = parent;
			this.accessCounters = accessCounters;
		}

		@Override
		protected Map<String, Float> delegate() {
			return parent;
		}

		@Override
		public Float get(Object key) {
			accessCounters.add((String)key);
			return super.get(key);
		}

		@Override
		public boolean containsKey(Object key) {
			accessCounters.add((String)key);
			return super.containsKey(key);
		}
	}

	private static class Tester {
		private final Map<String, Float> state = Maps.newHashMap();

		private Map<String, Float> result;

		private Multiset<String> accessCount;

		public Tester put(String key, float value) {
			state.put(key, value);
			return this;
		}

		public Tester run(IVarExpander expander) {
			final Multiset<String> counters = HashMultiset.create();
			this.result = expander.expand(new AccessCountingMap(ImmutableMap.copyOf(state), counters));
			this.accessCount = counters;
			return this;
		}

		public Tester run(EvaluatorFactory expander) {
			return run(expander.createExpander());
		}

		public Tester validate() {
			Assert.assertEquals(this.state, this.result);
			return this;
		}

		@SuppressWarnings("unused")
		public Tester checkAccessCount(String value, int count) {
			Assert.assertEquals(count, this.accessCount.count(value));
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
