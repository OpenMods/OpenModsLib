package openmods.model;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import java.util.Map;
import openmods.model.variant.Evaluator;
import openmods.model.variant.VariantModelState;
import org.junit.Assert;
import org.junit.Test;

public class VariantEvaluatorTest {

	private static class AccessCountingMap extends ForwardingMap<String, String> {
		private final Map<String, String> parent;

		private final Multiset<String> accessCounters;

		public AccessCountingMap(Map<String, String> parent, Multiset<String> accessCounters) {
			this.parent = parent;
			this.accessCounters = accessCounters;
		}

		@Override
		protected Map<String, String> delegate() {
			return parent;
		}

		@Override
		public String get(Object key) {
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
		private final Map<String, String> state = Maps.newHashMap();

		private Map<String, String> result;

		private Multiset<String> accessCount;

		public Tester put(String key, String value) {
			state.put(key, value);
			return this;
		}

		public Tester put(String key) {
			state.put(key, VariantModelState.DEFAULT_MARKER);
			return this;
		}

		public Tester clear(String key) {
			state.remove(key);
			return this;
		}

		public Tester run(Evaluator evaluator) {
			final Map<String, String> result = Maps.newHashMap(state);
			final Multiset<String> counters = HashMultiset.create();
			evaluator.expandVars(new AccessCountingMap(result, counters));
			this.result = result;
			this.accessCount = counters;
			return this;
		}

		public Tester validate() {
			Assert.assertEquals(this.state, this.result);
			return this;
		}

		public Tester checkAccessCount(String value, int count) {
			Assert.assertEquals(count, this.accessCount.count(value));
			return this;
		}
	}

	private static Tester start() {
		return new Tester();
	}

	@Test
	public void testKeyCopy() {
		Evaluator ev = new Evaluator();
		ev.addStatement("world := hello");
		start().put("hello").run(ev).put("world").validate();
	}

	@Test
	public void testKeyNegation() {
		Evaluator ev = new Evaluator();
		ev.addStatement("world := !hello");
		start().run(ev).put("world").validate();
	}

	@Test
	public void testKeyAnd() {
		Evaluator ev = new Evaluator();
		ev.addStatement("result := a & b");
		start().run(ev).validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testKeyOr() {
		Evaluator ev = new Evaluator();
		ev.addStatement("result := a | b");
		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testKeyXor() {
		Evaluator ev = new Evaluator();
		ev.addStatement("result := a ^ b");
		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testKeyEq() {
		Evaluator ev = new Evaluator();
		ev.addStatement("result := a = b");
		start().run(ev).put("result").validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testKeyToKeyValue() {
		Evaluator ev = new Evaluator();
		ev.addStatement("hello.world := true");
		start().put("true").run(ev).put("hello", "world").validate();
	}

	@Test
	public void testKeyValue() {
		Evaluator ev = new Evaluator();
		ev.addStatement("result := a.b");
		start().put("a", "b").run(ev).put("result").validate();
		start().put("a", "c").run(ev).validate();
		start().put("b").run(ev).validate();
		start().put("b", "b").run(ev).validate();
	}

	@Test
	public void testKeyValueToKeys() {
		Evaluator ev = new Evaluator();
		ev.addStatement("a := value.hello");
		ev.addStatement("b := value.world");
		start().put("value", "hello").run(ev).put("a").validate();
		start().put("value", "world").run(ev).put("b").validate();
	}

	@Test
	public void testKeyValueNegation() {
		Evaluator ev = new Evaluator();
		ev.addStatement("result := !a.b");
		start().put("a", "b").run(ev).validate();
		start().put("a", "c").run(ev).put("result").validate();
		start().put("b").run(ev).put("result").validate();
	}

	@Test
	public void testSingleMacro() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !x & y");
		ev.addStatement("result := f(a,b)");

		start().run(ev).validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testSingleMacroWithKeyValueArgs() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := x | y");
		ev.addStatement("result := f(v.a, v.b)");

		start().run(ev).validate();
		start().put("v", "a").run(ev).put("result").validate();
		start().put("v", "b").run(ev).put("result").validate();
		start().put("v", "c").run(ev).validate();
	}

	@Test
	public void testSingleMacroNegation() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !x & y");
		ev.addStatement("result := !f(a,b)");

		start().run(ev).put("result").validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testMacrosInExpressions() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !x & y");
		ev.addStatement("g(x,y) := x & !y");
		ev.addStatement("result := !(f(a,b) | g(a,b))");

		start().run(ev).put("result").validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testDeMorganLaw() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !(a|b)");
		ev.addStatement("g(x,y) := !a&!b");
		ev.addStatement("result := f(a,b) = g(a,b)");

		start().run(ev).put("result").validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testNestedMacros() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !x & y");
		ev.addStatement("g(x,y) := x & !y");
		ev.addStatement("xor(x,y) := !(f(x,y) | g(x,y))");
		ev.addStatement("result := xor(a,b)");

		start().run(ev).put("result").validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testMacroSymbolSeparation() {
		Evaluator ev = new Evaluator();
		ev.addStatement("a(a,b) := a | b");
		ev.addStatement("b(a,b) := a & b");
		ev.addStatement("result := a(a,b) & !b(a,b)");

		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testSequentialOperations() {
		Evaluator ev = new Evaluator();
		ev.addStatement("a := a | b");
		ev.addStatement("a := a | c");

		start().run(ev).validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).put("a").validate();
		start().put("c").run(ev).put("a").validate();
	}

	@Test
	public void testXorSwapOperations() {
		Evaluator ev = new Evaluator();
		ev.addStatement("a := a ^ b");
		ev.addStatement("b := a ^ b");
		ev.addStatement("a := a ^ b");

		start().run(ev).validate();
		start().put("a").run(ev).clear("a").put("b").validate();
		start().put("b").run(ev).clear("b").put("a").validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testMacroOverride() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := x & y");
		ev.addStatement("f(x,y) := !f(x,y)");
		ev.addStatement("result := f(a,b)");

		start().run(ev).put("result").validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testMacroNamespacesArgRename() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(a,b) := !a & b");
		ev.addStatement("result := f(b, a)");

		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testMacroNamespacesGlobalParamVisibility() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x) := x | global");
		ev.addStatement("result := f(a)");

		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("global").run(ev).put("result").validate();
		start().put("a").put("global").run(ev).put("result").validate();
	}

	@Test
	public void testMacroNamespacesArgKeyValue() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(a) := a.test");
		ev.addStatement("result := f(b)");

		start().run(ev).validate();
		start().put("a").run(ev).validate();
		start().put("a", "test").run(ev).validate();
		start().put("b", "not_test").run(ev).validate();
		start().put("b", "test").run(ev).put("result").validate();
	}

	@Test
	public void testConstantsInExpression() {
		Evaluator ev = new Evaluator();
		ev.addStatement("a := 1");
		ev.addStatement("b := 0");
		ev.addStatement("c := !1");
		ev.addStatement("d := !0");

		start().run(ev).put("a").put("c");
	}

	@Test
	public void testConstantsInMacro() {
		Evaluator ev = new Evaluator();
		ev.addStatement("true() := 1");
		ev.addStatement("false() := 0");
		ev.addStatement("a := true()");
		ev.addStatement("b := false()");
		ev.addStatement("c := !true()");
		ev.addStatement("d := !false()");

		start().run(ev).put("a").put("d");
	}

	@Test
	public void testAndConstantFolding() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a & 0");
			start().put("a").run(ev).validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a & 1");
			start().put("a").run(ev).put("result").validate().checkAccessCount("a", 1);
		}
	}

	@Test
	public void testAndSymbolMerging() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a & a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := (a & 1) & a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a & a.x");
			start().put("a").run(ev).put("result").validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a & a.x");
			start().put("a", "x").run(ev).put("result").validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !a & !a");
			start().run(ev).put("result").validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !!a & a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}
	}

	@Test
	public void testOrConstantFolding() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a | 1");
			start().put("a").run(ev).put("result").validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a | 0");
			start().put("a").run(ev).put("result").validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !a | !a");
			start().run(ev).put("result").validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !!a | a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}
	}

	@Test
	public void testOrSymbolMerging() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a | a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := (a | 0) | a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a | a.x");
			start().put("a").run(ev).validate().put("result").checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a | a.x");
			start().put("a", "x").run(ev).put("result").validate().checkAccessCount("a", 1);
		}
	}

	@Test
	public void testXorSymbolMerging() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a ^ a");
			start().run(ev).validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a ^ a ^ a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !a ^ !a");
			start().run(ev).validate().checkAccessCount("a", 0);
		}
	}

	@Test
	public void testEqSymbolMerging() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a = a");
			start().run(ev).put("result").validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a = a = a");
			start().run(ev).validate().checkAccessCount("a", 1);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !a = !a");
			start().run(ev).put("result").validate().checkAccessCount("a", 0);
		}
	}

	@Test
	public void checkConstantFoldingPropagation() {
		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := (a & 0) & a");
			start().put("a").run(ev).validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := a & (a & 0)");
			start().put("a").run(ev).validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !(a & 0)");
			start().put("a").run(ev).put("result").validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := !!(a & 0)");
			start().put("a").run(ev).validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := (a & !1) & a");
			start().put("a").run(ev).validate().checkAccessCount("a", 0);
		}

		{
			Evaluator ev = new Evaluator();
			ev.addStatement("result := (a | 1) & a");
			start().put("a").run(ev).put("result").validate().checkAccessCount("a", 1);
		}
	}

}
