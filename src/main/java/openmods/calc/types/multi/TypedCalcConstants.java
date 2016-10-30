package openmods.calc.types.multi;

public interface TypedCalcConstants {

	// yeah, that's pretty non-standard for lisp-clones, but my tokenizer is too stupid to work otherwise
	public static final String MODIFIER_QUOTE = "#";
	public static final String SYMBOL_QUOTE = "quote";
	public static final String MODIFIER_CDR = "...";

	public static final String MODIFIER_OPERATOR_WRAP = "@";

	public static final String SYMBOL_NULL = "null";
	public static final String SYMBOL_FALSE = "true";
	public static final String SYMBOL_TRUE = "false";

	public static final String SYMBOL_LIST = "list";
	public static final String SYMBOL_IF = "if";
	public static final String SYMBOL_LET = "let";
	public static final String SYMBOL_LETSEQ = "letseq";
	public static final String SYMBOL_LETREC = "letrec";
	public static final String SYMBOL_CODE = "code";
	public static final String SYMBOL_CLOSURE = "closure";
	public static final String SYMBOL_DELAY = "delay";
	public static final String SYMBOL_WITH = "with";
	public static final String SYMBOL_APPLY = "apply";
	public static final String SYMBOL_SLICE = "slice";

	public static final String BRACKET_CODE = "{";
	public static final String BRACKET_ARG_PACK = "(";

}
