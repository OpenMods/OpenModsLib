package openmods.calc;

import java.util.Arrays;
import openmods.calc.types.multi.StringInterpolate;
import openmods.calc.types.multi.StringInterpolate.TemplatePartInfo;
import openmods.calc.types.multi.StringInterpolate.TemplatePartType;
import org.junit.Assert;
import org.junit.Test;

public class StringInterpolateParserTest {

	private static TemplatePartInfo v(String var) {
		return new TemplatePartInfo(TemplatePartType.VAR, var);
	}

	private static TemplatePartInfo c(String contents) {
		return new TemplatePartInfo(TemplatePartType.CONST, contents);
	}

	private static final TemplatePartInfo BRACKET_START = new TemplatePartInfo(TemplatePartType.BRACKET_START, "{");
	private static final TemplatePartInfo BRACKET_END = new TemplatePartInfo(TemplatePartType.BRACKET_END, "}");

	private static void check(String input, TemplatePartInfo... result) {
		Assert.assertEquals(Arrays.asList(result), StringInterpolate.parseTemplate(input));
	}

	private static void expectFail(String input) {
		StringInterpolate.parseTemplate(input);
		Assert.fail();
	}

	@Test
	public void forEmptyStringShouldReturnEmptyList() {
		check("");
	}

	@Test
	public void forSimpleStringShouldReturnConstant() {
		check("a", c("a"));
		check("ab", c("ab"));
		check("abc", c("abc"));
	}

	@Test
	public void forNonEmptyVarShouldReturnVar() {
		check("{a}", v("a"));
		check("{ab}", v("ab"));
		check("{abc}", v("abc"));
	}

	@Test
	public void forConstBeforeVarShouldReturnBoth() {
		check("a{b}", c("a"), v("b"));
		check("ab{c}", c("ab"), v("c"));
		check("a{bc}", c("a"), v("bc"));
		check("ab{cd}", c("ab"), v("cd"));
	}

	@Test
	public void forConstAfterVarShouldReturnBoth() {
		check("{a}b", v("a"), c("b"));
		check("{a}bc", v("a"), c("bc"));
		check("{ab}c", v("ab"), c("c"));
		check("{ab}cd", v("ab"), c("cd"));
	}

	@Test
	public void forConstsAroundVarShouldReturnAll() {
		check("a{b}c", c("a"), v("b"), c("c"));
		check("ab{cd}ef", c("ab"), v("cd"), c("ef"));
	}

	@Test
	public void forVarsAroundConstShouldReturnAll() {
		check("{a}b{c}", v("a"), c("b"), v("c"));
		check("{ab}cd{ef}", v("ab"), c("cd"), v("ef"));
	}

	@Test
	public void forSingleQuotedOpenBracketShouldReturn() {
		check("{{", BRACKET_START);
	}

	@Test
	public void forTwoQuotedOpenBracketShouldReturn() {
		check("{{{{", BRACKET_START, BRACKET_START);
	}

	@Test
	public void forTextBeforeQuotedOpenBracketShouldReturn() {
		check("a{{", c("a"), BRACKET_START);
	}

	@Test
	public void forTextAfterQuotedOpenBracketShouldReturn() {
		check("{{a", BRACKET_START, c("a"));
	}

	@Test
	public void forSingleQuotedCloseBracketShouldReturn() {
		check("}}", BRACKET_END);
	}

	@Test
	public void forTwoQuotedCloseBracketShouldReturn() {
		check("}}}}", BRACKET_END, BRACKET_END);
	}

	@Test
	public void forTextBeforeQuotedCloseBracketShouldReturn() {
		check("a}}", c("a"), BRACKET_END);
	}

	@Test
	public void forTextAfterQuotedCloseBracketShouldReturn() {
		check("}}a", BRACKET_END, c("a"));
	}

	@Test
	public void forSingleQuotedBracketsShouldReturn() {
		check("{{}}", BRACKET_START, BRACKET_END);
	}

	@Test
	public void forSingleQuotedBracketsWithTextShouldReturn() {
		check("{{a}}", BRACKET_START, c("a"), BRACKET_END);
	}

	@Test
	public void forSingleQuotedBracketsWithTextAroundShouldReturn() {
		check("a{{}}b", c("a"), BRACKET_START, BRACKET_END, c("b"));
	}

	@Test
	public void forVarInsideQuotedBracketsShouldReturn() {
		check("{{{a}}}", BRACKET_START, v("a"), BRACKET_END);
	}

	@Test(expected = IllegalStateException.class)
	public void forEmptyVarShouldThrow() {
		expectFail("{}");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedOpenBracketShouldThrow() {
		expectFail("{");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnclosedVarShouldThrow() {
		expectFail("{a");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedOpenBracketAfterTextAfterShouldThrow() {
		expectFail("a{");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedCloseBracketShouldThrow() {
		expectFail("}");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedCloseBracketWithTextAfterShouldThrow() {
		expectFail("}a");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnopenedVarShouldThrow() {
		expectFail("a}");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedCloseBracketAfterQuotedOpenBracketShouldThrow() {
		expectFail("{{}");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedCloseBracketAfterQuotedOpenBracketWithTextShouldThrow() {
		expectFail("{{a}");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedOpenBracketBeforeQuotedCloseBracketShouldThrow() {
		expectFail("{}}");
	}

	@Test(expected = IllegalStateException.class)
	public void forUnmatchedOpenBracketBeforeQuotedCloseBracketWithTextShouldThrow() {
		expectFail("{a}}");
	}

}
