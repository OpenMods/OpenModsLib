package openmods.calc;

import static openmods.calc.TokenTestUtils.op;

public class ParserTestUtils {
	public static class DummyBinaryOperator<E> extends BinaryOperator<E> {
		private final String id;

		public DummyBinaryOperator(int precendence, String id) {
			super(precendence);
			this.id = id;
		}

		@Override
		public String toString() {
			return "Binary operator [" + id + "]";
		}

		@Override
		protected E execute(E left, E right) {
			return null;
		}
	}

	public static class DummyUnaryOperator<E> extends UnaryOperator<E> {
		private final String id;

		public DummyUnaryOperator(int precendence, String id) {
			super(precendence);
			this.id = id;
		}

		@Override
		public String toString() {
			return "Unary operator [" + id + "]";
		}

		@Override
		protected E execute(E value) {
			return null;
		}

	}

	public static final BinaryOperator<String> PLUS = new DummyBinaryOperator<String>(1, "+");

	public static final Token OP_PLUS = op("+");

	public static final UnaryOperator<String> UNARY_PLUS = new DummyUnaryOperator<String>(4, "u+");

	public static final BinaryOperator<String> MINUS = new DummyBinaryOperator<String>(1, "-");

	public static final Token OP_MINUS = op("-");

	public static final UnaryOperator<String> UNARY_MINUS = new DummyUnaryOperator<String>(4, "u-");

	public static final UnaryOperator<String> UNARY_NEG = new DummyUnaryOperator<String>(4, "u!");

	public static final Token OP_NEG = op("!");

	public static final BinaryOperator<String> MULTIPLY = new DummyBinaryOperator<String>(2, "*");

	public static final Token OP_MULTIPLY = op("*");

	public static IExecutable<String> c(String value) {
		return new Value<String>(value);
	}

	public static SymbolReference<String> s(String value) {
		return new SymbolReference<String>(value);
	}

	public static SymbolReference<String> s(String value, int args) {
		return new SymbolReference<String>(value, args, 1);
	}

	public static SymbolReference<String> s(String value, int args, int rets) {
		return new SymbolReference<String>(value, args, rets);
	}

	public static final IValueParser<String> VALUE_PARSER = new IValueParser<String>() {
		@Override
		public String parseToken(Token token) {
			return token.value;
		}
	};
}
