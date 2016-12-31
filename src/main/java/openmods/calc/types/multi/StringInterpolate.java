package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import openmods.calc.Frame;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.IValuePrinter;
import openmods.calc.SymbolMap;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SingleStateTransition;
import openmods.calc.parsing.SingleTokenPostfixCompilerState;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;

public class StringInterpolate {

	private static interface ITemplatePart {
		public void append(IValuePrinter<TypedValue> printer, SymbolMap<TypedValue> symbols, StringBuilder output);
	}

	private static final ITemplatePart BRACKET_START_PART = new ITemplatePart() {
		@Override
		public void append(IValuePrinter<TypedValue> printer, SymbolMap<TypedValue> symbols, StringBuilder output) {
			output.append('{');
		}
	};

	private static final ITemplatePart BRACKET_END_PART = new ITemplatePart() {
		@Override
		public void append(IValuePrinter<TypedValue> printer, SymbolMap<TypedValue> symbols, StringBuilder output) {
			output.append('}');
		}
	};

	public enum TemplatePartType {
		CONST {
			@Override
			protected ITemplatePart createPart(final String contents) {
				return new ITemplatePart() {
					@Override
					public void append(IValuePrinter<TypedValue> printer, SymbolMap<TypedValue> symbols, StringBuilder output) {
						output.append(contents);
					}
				};
			}
		},
		VAR {
			@Override
			protected ITemplatePart createPart(final String contents) {
				return new ITemplatePart() {
					@Override
					public void append(IValuePrinter<TypedValue> printer, SymbolMap<TypedValue> symbols, StringBuilder output) {
						final ISymbol<TypedValue> value = symbols.get(contents);
						Preconditions.checkArgument(value != null, "No symbol: " + contents);
						output.append(printer.str(value.get()));
					}
				};
			}
		},
		BRACKET_START {
			@Override
			protected ITemplatePart createPart(String contents) {
				Preconditions.checkArgument(contents.equals("{"), "Expected {, got %s", contents);
				return BRACKET_START_PART;
			}
		},
		BRACKET_END {
			@Override
			protected ITemplatePart createPart(String contents) {
				Preconditions.checkArgument(contents.equals("}"), "Expected }, got %s", contents);
				return BRACKET_END_PART;
			}
		};

		protected abstract ITemplatePart createPart(String contents);
	}

	public static class TemplatePartInfo {
		public final TemplatePartType type;
		public final String contents;

		public TemplatePartInfo(TemplatePartType type, String contents) {
			this.type = type;
			this.contents = contents;
		}

		public ITemplatePart createPart() {
			return type.createPart(contents);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((contents == null)? 0 : contents.hashCode());
			result = prime * result + ((type == null)? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj instanceof TemplatePartInfo) {
				final TemplatePartInfo other = (TemplatePartInfo)obj;
				return Objects.equal(this.contents, other.contents)
						&& this.type == other.type;
			}

			return false;
		}

		@Override
		public String toString() {
			return type + ":" + contents;
		}

	}

	private static final TemplatePartInfo BRACKET_START_INFO = new TemplatePartInfo(TemplatePartType.BRACKET_START, "{");
	private static final TemplatePartInfo BRACKET_END_INFO = new TemplatePartInfo(TemplatePartType.BRACKET_END, "}");

	private enum ParsingState {
		OUTSIDE_QUOTE, BRACKET_START, BRACKET_END, INSIDE_VAR_BRACKET
	}

	public static List<TemplatePartInfo> parseTemplate(String template) {
		final List<TemplatePartInfo> parts = Lists.newArrayList();
		// if (template.isEmpty())
		// return parts;

		int sectionStart = 0;
		ParsingState state = ParsingState.OUTSIDE_QUOTE;
		for (int currentPos = 0; currentPos < template.length(); currentPos++) {
			final char ch = template.charAt(currentPos);
			switch (state) {
				case OUTSIDE_QUOTE: {
					if (ch == '{') {
						state = ParsingState.BRACKET_START;
						if (sectionStart < currentPos)
							parts.add(new TemplatePartInfo(TemplatePartType.CONST, template.substring(sectionStart, currentPos)));
					} else if (ch == '}') {
						state = ParsingState.BRACKET_END;
						if (sectionStart < currentPos)
							parts.add(new TemplatePartInfo(TemplatePartType.CONST, template.substring(sectionStart, currentPos)));
					}
					break;
				}
				case BRACKET_START: {
					// note: explicitly treating empty brackets as invalid
					if (ch == '{') {
						state = ParsingState.OUTSIDE_QUOTE;
						parts.add(BRACKET_START_INFO);
						// current char is '{' - so skip
						sectionStart = currentPos + 1;
					} else if (ch == '}') {
						throw new IllegalStateException("Unmatched bracket");
					} else {
						state = ParsingState.INSIDE_VAR_BRACKET;
						// current char is first element of var - don't skip
						sectionStart = currentPos;
					}
					break;
				}
				case BRACKET_END: {
					if (ch == '}') {
						state = ParsingState.OUTSIDE_QUOTE;
						parts.add(BRACKET_END_INFO);
						// current char is '}' - so skip
						sectionStart = currentPos + 1;
					} else {
						throw new IllegalStateException("Unmatched bracket");
					}
					break;
				}

				case INSIDE_VAR_BRACKET: {
					if (ch == '}') {
						state = ParsingState.OUTSIDE_QUOTE;
						parts.add(new TemplatePartInfo(TemplatePartType.VAR, template.substring(sectionStart, currentPos)));
						// current char is '}' - so skip
						sectionStart = currentPos + 1;
					}
					break;
				}
				default:
					throw new IllegalStateException();
			}
		}
		Preconditions.checkState(state == ParsingState.OUTSIDE_QUOTE, "Invalid brackets");
		if (sectionStart < template.length())
			parts.add(new TemplatePartInfo(TemplatePartType.CONST, template.substring(sectionStart)));

		return parts;
	}

	private static class StringInterpolateExecutable implements IExecutable<TypedValue> {

		private final TypeDomain domain;
		private final IValuePrinter<TypedValue> printer;
		private final List<ITemplatePart> parts;

		public StringInterpolateExecutable(TypeDomain domain, IValuePrinter<TypedValue> printer, String template) {
			this.domain = domain;
			this.printer = printer;
			this.parts = ImmutableList.copyOf(Iterables.transform(parseTemplate(template),
					new Function<TemplatePartInfo, ITemplatePart>() {
						@Override
						public ITemplatePart apply(TemplatePartInfo input) {
							return input.createPart();
						}
					}));
		}

		@Override
		public void execute(Frame<TypedValue> frame) {
			final SymbolMap<TypedValue> symbols = frame.symbols();
			final StringBuilder buffer = new StringBuilder();
			for (ITemplatePart part : parts)
				part.append(printer, symbols, buffer);

			frame.stack().push(domain.create(String.class, buffer.toString()));
		}
	}

	public static class StringInterpolateModifier extends SingleStateTransition.ForModifier<TypedValue> {
		private final TypeDomain domain;
		private final IValuePrinter<TypedValue> printer;

		public StringInterpolateModifier(TypeDomain domain, IValuePrinter<TypedValue> printer) {
			this.domain = domain;
			this.printer = printer;
		}

		@Override
		public IExprNode<TypedValue> createRootNode(IExprNode<TypedValue> child) {
			return child;
		}

		@Override
		public IExprNode<TypedValue> parseSymbol(ICompilerState<TypedValue> state, PeekingIterator<Token> input) {
			final Token token = input.next();
			Preconditions.checkState(token.type == TokenType.STRING, "Expected string token, got %s", token);

			final String template = token.value;
			return new IExprNode<TypedValue>() {

				@Override
				public Iterable<IExprNode<TypedValue>> getChildren() {
					return ImmutableList.of();
				}

				@Override
				public void flatten(List<IExecutable<TypedValue>> output) {
					output.add(new StringInterpolateExecutable(domain, printer, template));
				}
			};
		}
	}

	public static class StringInterpolatePostfixCompilerState extends SingleTokenPostfixCompilerState<TypedValue> {

		private final TypeDomain domain;
		private final IValuePrinter<TypedValue> printer;

		public StringInterpolatePostfixCompilerState(TypeDomain domain, IValuePrinter<TypedValue> printer) {
			this.domain = domain;
			this.printer = printer;
		}

		@Override
		protected IExecutable<TypedValue> parseToken(Token token) {
			Preconditions.checkState(token.type == TokenType.STRING, "Expected string token, got %s", token);
			return new StringInterpolateExecutable(domain, printer, token.value);
		}

	}

}
