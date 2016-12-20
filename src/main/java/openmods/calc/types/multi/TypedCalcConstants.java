package openmods.calc.types.multi;

public interface TypedCalcConstants {

	// yeah, that's pretty non-standard for lisp-clones, but my tokenizer is too stupid to work otherwise
	public static final String MODIFIER_QUOTE = "#";
	public static final String SYMBOL_QUOTE = "quote";
	public static final String MODIFIER_CDR = "...";
	public static final String MODIFIER_INTERPOLATE = "$";
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
	public static final String SYMBOL_CLOSURE_VAR = "closurevar";
	public static final String SYMBOL_DELAY = "delay";
	public static final String SYMBOL_WITH = "with";
	public static final String SYMBOL_APPLY = "apply";
	public static final String SYMBOL_SLICE = "slice";
	public static final String SYMBOL_CASE = "case";
	public static final String SYMBOL_MATCH = "match";
	public static final String SYMBOL_AND_THEN = "andthen";
	public static final String SYMBOL_OR_ELSE = "orelse";
	public static final String SYMBOL_NON_NULL = "nonnull";
	public static final String SYMBOL_NULL_EXECUTE = "nexecute";
	public static final String SYMBOL_CONSTANT = "const";
	public static final String SYMBOL_ALT = "alt";
	public static final String SYMBOL_DO = "do";
	public static final String SYMBOL_PATTERN = "pattern";

	public static final String BRACKET_CODE = "{";
	public static final String BRACKET_ARG_PACK = "(";

	public static final String MATCH_ANY = "_";

	public static final String SLOT_BOOL = "bool";
	public static final String SLOT_LENGTH = "len";
	public static final String SLOT_ATTRIBUTE = "attribute";
	public static final String SLOT_EQUAL = "equal";
	public static final String SLOT_CALL = "call";
	public static final String SLOT_TYPE = "type";
	public static final String SLOT_SLICE = "slice";
	public static final String SLOT_STR = "str";
	public static final String SLOT_REPR = "repr";
}
