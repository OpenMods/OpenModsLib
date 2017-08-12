package openmods.model.eval;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import info.openmods.calc.parsing.token.Token;
import info.openmods.calc.parsing.token.TokenIterator;
import info.openmods.calc.parsing.token.TokenType;
import info.openmods.calc.parsing.token.Tokenizer;
import info.openmods.calc.types.fp.DoubleParser;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.animation.IAnimatedModel;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;
import org.apache.commons.lang3.tuple.ImmutablePair;
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

	private static class ArgExpander implements IExpander {

		private final List<Pair<String, Float>> expansions;

		public ArgExpander(List<Pair<String, Float>> expansions) {
			this.expansions = ImmutableList.copyOf(expansions);
		}

		@Override
		public Map<String, Float> expand(Map<String, Float> args) {
			final Map<String, Float> result = Maps.newHashMap(args);
			for (Pair<String, Float> e : expansions)
				result.put(e.getKey(), e.getValue());

			return ImmutableMap.copyOf(result);
		}

	}

	// TODO make it powerful

	private final Tokenizer tokenizer = new Tokenizer();

	{
		tokenizer.addOperator(":=");
	}

	private final List<Pair<String, String>> clipArgs = Lists.newArrayList();

	private final List<Pair<String, Float>> constArgs = Lists.newArrayList();

	private static Token nextToken(Iterator<Token> tokens) {
		Preconditions.checkState(tokens.hasNext(), "Unexpected end of statement");
		return tokens.next();
	}

	private static String expectToken(Iterator<Token> tokens, TokenType type) {
		Token result = nextToken(tokens);
		Preconditions.checkState(result.type == type, "Expected %s, got %s", type, result);
		return result.value;
	}

	private static void expectToken(Iterator<Token> tokens, TokenType type, String value) {
		Token result = nextToken(tokens);
		Preconditions.checkState(result.type == type && result.value.equals(value), "Expect %s:%s, got %s", type, value, result);
	}

	private static Token expectTokens(Iterator<Token> tokens, TokenType... types) {
		Token result = nextToken(tokens);
		Preconditions.checkState(ImmutableSet.of(types).contains(types), "Expect %s, got %s", Arrays.toString(types), result);
		return result;
	}

	private final DoubleParser numberParser = new DoubleParser();

	public void appendStatement(String statement) {
		try {
			final TokenIterator tokens = tokenizer.tokenize(statement);
			final String id = expectToken(tokens, TokenType.SYMBOL);

			final Token op = expectTokens(tokens, TokenType.OPERATOR, TokenType.LEFT_BRACKET);

			switch (op.type) {
				case OPERATOR: {
					final Token token = nextToken(tokens);
					Preconditions.checkState(token.type.isNumber(), "Expected number, got '%s'", token.value);
					final Double value = numberParser.parseToken(token);
					constArgs.add(ImmutablePair.of(id, value.floatValue()));
					break;
				}
				case LEFT_BRACKET: {
					Preconditions.checkState(op.value.equals("("), "Invalid brackets");
					final String value = expectToken(tokens, TokenType.SYMBOL);
					expectToken(tokens, TokenType.RIGHT_BRACKET, ")");
					clipArgs.add(ImmutablePair.of(id, value));
					break;
				}
				default:
					throw new IllegalArgumentException("Expected either 'clip(arg)' of 'arg := number'");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse: " + statement, e);
		}
	}

	public IEvaluator createEvaluator(IModel model) {
		if (!(model instanceof IAnimatedModel)) return EMPTY;

		final IAnimatedModel animatedModel = (IAnimatedModel)model;

		final List<Pair<IClip, String>> args = Lists.newArrayList();

		for (Pair<String, String> clipArg : clipArgs) {
			final String clipName = clipArg.getKey();
			final String arg = clipArg.getValue();
			final Optional<? extends IClip> clip = animatedModel.getClip(clipName);
			Preconditions.checkState(clip.isPresent(), "Can't find clip '%s'", clipName);
			args.add(ImmutablePair.<IClip, String> of(clip.get(), arg));
		}

		return new ClipEvaluator(args);
	}

	public IExpander createExpander() {
		Preconditions.checkState(clipArgs.isEmpty(), "No transforms allowed in expander");
		return new ArgExpander(constArgs);
	}

}
