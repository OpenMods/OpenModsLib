package openmods.model.eval;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.animation.IAnimatedModel;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;
import org.apache.commons.lang3.tuple.Pair;

public class EvaluatorFactory {

	private static final IEvaluator EMPTY = new IEvaluator() {
		@Override
		public IModelState evaluate(Map<String, Float> args) {
			return TRSRTransformation.identity();
		}
	};

	private static class ClipEvaluator implements IEvaluator {

		private final List<Pair<IClip, String>> clips;

		public ClipEvaluator(List<Pair<IClip, String>> clips) {
			this.clips = ImmutableList.copyOf(clips);
		}

		@Override
		public IModelState evaluate(final Map<String, Float> args) {
			return new IModelState() {
				@Override
				public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
					if (!part.isPresent()) return Optional.absent();

					final IModelPart maybeJoint = part.get();
					if (!(maybeJoint instanceof IJoint)) return Optional.absent();

					final IJoint joint = (IJoint)part.get();

					TRSRTransformation result = TRSRTransformation.identity();

					for (Pair<IClip, String> clip : clips) {
						final Float maybeArg = args.get(clip.getRight());
						final float arg = maybeArg != null? maybeArg : 0;

						// TODO bone transforms
						final TRSRTransformation clipTransform = clip.getLeft().apply(joint).apply(arg);
						result = result.compose(clipTransform);
					}

					return Optional.of(result);
				}
			};
		}

	}

	// TODO make it powerful
	private static final Pattern SIMPLE_EXPR = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]+)\\(([a-zA-Z_][a-zA-Z0-9_]+)\\)");

	private final List<Pair<String, String>> clipArgs = Lists.newArrayList();

	public void appendStatement(String statement) {
		final Matcher matcher = SIMPLE_EXPR.matcher(statement);
		Preconditions.checkState(matcher.matches(), "Invalid statement, expected 'clip(arg)'");
		final String clip = matcher.group(1);
		final String arg = matcher.group(2);

		clipArgs.add(Pair.of(clip, arg));
	}

	public IEvaluator bind(IModel model) {
		if (!(model instanceof IAnimatedModel)) return EMPTY;

		final IAnimatedModel animatedModel = (IAnimatedModel)model;

		final List<Pair<IClip, String>> args = Lists.newArrayList();

		for (Pair<String, String> clipArg : clipArgs) {
			final String clipName = clipArg.getKey();
			final String arg = clipArg.getValue();
			final Optional<? extends IClip> clip = animatedModel.getClip(clipName);
			Preconditions.checkState(clip.isPresent(), "Can't find clip '%s'", clipName);
			args.add(Pair.<IClip, String> of(clip.get(), arg));
		}

		return new ClipEvaluator(args);
	}

}
