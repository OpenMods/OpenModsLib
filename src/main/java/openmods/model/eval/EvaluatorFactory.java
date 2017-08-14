package openmods.model.eval;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;

public class EvaluatorFactory {

	public interface IClipProvider {
		public Optional<? extends IClip> get(String name);
	}

	private static interface ITransformExecutor {
		public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args);
	}

	private static final ITransformExecutor EMPTY_TRANSFORM_EXECUTOR = new ITransformExecutor() {
		@Override
		public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
			return initial;
		}
	};

	private static interface IValueExecutor {
		public void apply(Map<String, Float> args);
	}

	private static final IValueExecutor EMPTY_VALUE_EXECUTOR = new IValueExecutor() {
		@Override
		public void apply(Map<String, Float> args) {}
	};

	private static interface IParam {
		public float evaluate(Map<String, Float> args);
	}

	private static IParam arg(final String name) {
		return new IParam() {
			@Override
			public float evaluate(Map<String, Float> args) {
				final Float result = args.get(name);
				return result != null? result : 0;
			}
		};
	}

	private static IParam constParam(final float value) {
		return new IParam() {
			@Override
			public float evaluate(Map<String, Float> args) {
				return value;
			}
		};
	}

	private final Tokenizer tokenizer = new Tokenizer();

	{
		tokenizer.addOperator(":=");
	}

	private interface IStatement {
		public ITransformExecutor bind(IClipProvider provider);

		public IValueExecutor free();
	}

	private static class AssignStatement implements IStatement {
		private final String name;
		private final IParam value;

		public AssignStatement(String name, IParam value) {
			this.name = name;
			this.value = value;
		}

		private void eval(Map<String, Float> args) {
			final Float v = value.evaluate(args);
			args.put(name, v);
		}

		@Override
		public ITransformExecutor bind(IClipProvider provider) {
			return new ITransformExecutor() {
				@Override
				public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
					eval(args);
					return initial;
				}
			};
		}

		@Override
		public IValueExecutor free() {
			return new IValueExecutor() {
				@Override
				public void apply(Map<String, Float> args) {
					eval(args);
				}
			};
		}
	}

	private static class ClipStatement implements IStatement {

		private final String clipName;
		private final IParam param;

		public ClipStatement(String clipName, IParam param) {
			this.clipName = clipName;
			this.param = param;
		}

		@Override
		public ITransformExecutor bind(IClipProvider provider) {
			final Optional<? extends IClip> clip = provider.get(clipName);
			Preconditions.checkState(clip.isPresent(), "Can't find clip '%s'", clipName);
			return createForClip(clip.get(), param);
		}

		@Override
		public IValueExecutor free() {
			throw new UnsupportedOperationException("Clip cannot be applied in this context");
		}

	}

	private final List<IStatement> statements = Lists.newArrayList();

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
					statements.add(new AssignStatement(id, constParam(value.floatValue())));
					break;
				}
				case LEFT_BRACKET: {
					Preconditions.checkState(op.value.equals("("), "Invalid brackets");
					final String value = expectToken(tokens, TokenType.SYMBOL);
					expectToken(tokens, TokenType.RIGHT_BRACKET, ")");
					statements.add(new ClipStatement(id, arg(value)));
					break;
				}
				default:
					throw new IllegalArgumentException("Expected either 'clip(arg)' of 'arg := number'");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse: " + statement, e);
		}
	}

	private static ITransformExecutor composeTransformExecutors(List<ITransformExecutor> contents) {
		if (contents.isEmpty()) return EMPTY_TRANSFORM_EXECUTOR;
		if (contents.size() == 1)
			return contents.get(0);

		final List<ITransformExecutor> executors = ImmutableList.copyOf(contents);
		return new ITransformExecutor() {

			@Override
			public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
				TRSRTransformation result = initial;
				for (ITransformExecutor e : executors)
					result = e.apply(result, joint, args);

				return result;
			}
		};
	}

	private static IValueExecutor composeValueExecutors(List<IValueExecutor> contents) {
		if (contents.isEmpty()) return EMPTY_VALUE_EXECUTOR;
		if (contents.size() == 1)
			return contents.get(0);

		final List<IValueExecutor> executors = ImmutableList.copyOf(contents);
		return new IValueExecutor() {
			@Override
			public void apply(Map<String, Float> args) {
				for (IValueExecutor executors : executors)
					executors.apply(args);
			}
		};
	}

	private static ITransformExecutor createForClip(final IClip clip, final IParam param) {
		return new ITransformExecutor() {
			@Override
			public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
				final float paramValue = param.evaluate(args);
				final TRSRTransformation clipTransform = clip.apply(joint).apply(paramValue);
				return initial.compose(clipTransform);
			}
		};
	}

	private static class EvaluatorImpl implements ITransformEvaluator {

		private final ITransformExecutor executor;

		public EvaluatorImpl(ITransformExecutor executor) {
			this.executor = executor;
		}

		@Override
		public TRSRTransformation evaluate(IJoint joint, Map<String, Float> args) {
			final Map<String, Float> mutableArgs = Maps.newHashMap(args);
			return executor.apply(TRSRTransformation.identity(), joint, mutableArgs);
		}
	}

	private static final ITransformEvaluator EMPTY_EVALUATOR = new ITransformEvaluator() {
		@Override
		public TRSRTransformation evaluate(IJoint joint, Map<String, Float> args) {
			return TRSRTransformation.identity();
		}
	};

	public ITransformEvaluator createEvaluator(IClipProvider provider) {
		if (statements.isEmpty())
			return EMPTY_EVALUATOR;

		final List<ITransformExecutor> executors = Lists.newArrayList();

		for (IStatement statement : statements)
			executors.add(statement.bind(provider));

		return new EvaluatorImpl(composeTransformExecutors(executors));
	}

	private static final IVarExpander EMPTY_EXPANDER = new IVarExpander() {
		@Override
		public Map<String, Float> expand(Map<String, Float> args) {
			return args;
		}
	};

	private static class ExpanderImpl implements IVarExpander {

		private final IValueExecutor executor;

		public ExpanderImpl(IValueExecutor executor) {
			this.executor = executor;
		}

		@Override
		public Map<String, Float> expand(Map<String, Float> args) {
			final Map<String, Float> mutableArgs = Maps.newHashMap(args);
			executor.apply(mutableArgs);
			return mutableArgs;
		}

	}

	public IVarExpander createExpander() {
		if (statements.isEmpty())
			return EMPTY_EXPANDER;
		final List<IValueExecutor> executors = Lists.newArrayList();

		for (IStatement statement : statements)
			executors.add(statement.free());

		return new ExpanderImpl(composeValueExecutors(executors));
	}

}
