package openmods.model;

import com.google.common.collect.Maps;
import java.util.Map;
import openmods.model.variant.Evaluator;
import openmods.model.variant.VariantModelState;
import org.junit.Assert;
import org.junit.Test;

public class VariantEvaluatorTest {

	private static class Tester {
		private final Map<String, String> state = Maps.newHashMap();

		private Map<String, String> result;

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
			evaluator.expandVars(result);
			this.result = result;
			return this;
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
	public void testSingleFunction() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !x & y");
		ev.addStatement("result := f(a,b)");

		start().run(ev).validate();
		start().put("a").run(ev).validate();
		start().put("b").run(ev).put("result").validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testSingleFunctionWithKeyValueArgs() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := x | y");
		ev.addStatement("result := f(v.a, v.b)");

		start().run(ev).validate();
		start().put("v", "a").run(ev).put("result").validate();
		start().put("v", "b").run(ev).put("result").validate();
		start().put("v", "c").run(ev).validate();
	}

	@Test
	public void testSingleFunctionNegation() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x,y) := !x & y");
		ev.addStatement("result := !f(a,b)");

		start().run(ev).put("result").validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).put("result").validate();
	}

	@Test
	public void testFunctionsInExpressions() {
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
	public void testNestedFunctions() {
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
	public void testFunctionSymbolSeparation() {
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
	public void testFunctionOverride() {
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
	public void testFunctionNamespacesArgRename() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(a,b) := !a & b");
		ev.addStatement("result := f(b, a)");

		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("b").run(ev).validate();
		start().put("a").put("b").run(ev).validate();
	}

	@Test
	public void testFunctionNamespacesGlobalParamVisibility() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(x) := x | global");
		ev.addStatement("result := f(a)");

		start().run(ev).validate();
		start().put("a").run(ev).put("result").validate();
		start().put("global").run(ev).put("result").validate();
		start().put("a").put("global").run(ev).put("result").validate();
	}

	@Test
	public void testFunctionNamespacesArgKeyValue() {
		Evaluator ev = new Evaluator();
		ev.addStatement("f(a) := a.test");
		ev.addStatement("result := f(b)");

		start().run(ev).validate();
		start().put("a").run(ev).validate();
		start().put("a", "test").run(ev).validate();
		start().put("b", "not_test").run(ev).validate();
		start().put("b", "test").run(ev).put("result").validate();
	}
}
