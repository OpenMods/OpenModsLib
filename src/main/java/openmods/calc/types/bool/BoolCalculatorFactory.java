package openmods.calc.types.bool;

import openmods.calc.BinaryOperator;
import openmods.calc.Calculator;
import openmods.calc.Environment;
import openmods.calc.ExprType;
import openmods.calc.IValuePrinter;
import openmods.calc.OperatorDictionary;
import openmods.calc.SimpleCalculatorFactory;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.BasicCompilerMapFactory;
import openmods.calc.parsing.CommonSimpleSymbolFactory;
import openmods.calc.parsing.IValueParser;

public class BoolCalculatorFactory<M> extends SimpleCalculatorFactory<Boolean, M> {

	@Override
	protected IValueParser<Boolean> getValueParser() {
		return new BoolParser();
	}

	@Override
	protected Boolean getNullValue() {
		return Boolean.FALSE;
	}

	@Override
	protected IValuePrinter<Boolean> createValuePrinter() {
		return new BoolPrinter();
	}

	@Override
	protected void configureEnvironment(Environment<Boolean> env) {
		env.setGlobalSymbol("true", Boolean.TRUE);
		env.setGlobalSymbol("false", Boolean.FALSE);
	}

	private static final int PRIORITY_AND = 4; // &
	private static final int PRIORITY_OR = 3; // |
	private static final int PRIORITY_COMPARE = 2; // ^, =, =>
	private static final int PRIORITY_COLON = 1;

	private static class OpAnd extends BinaryOperator.Direct<Boolean> {
		private OpAnd(String id) {
			super(id, PRIORITY_AND);
		}

		@Override
		public Boolean execute(Boolean left, Boolean right) {
			return left & right;
		}
	}

	private static class OpOr extends BinaryOperator.Direct<Boolean> {
		private OpOr(String id) {
			super(id, PRIORITY_OR);
		}

		@Override
		public Boolean execute(Boolean left, Boolean right) {
			return left | right;
		}
	}

	private static class OpImplies extends BinaryOperator.Direct<Boolean> {
		private OpImplies(String id) {
			super(id, PRIORITY_COMPARE);
		}

		@Override
		public Boolean execute(Boolean left, Boolean right) {
			return !left | right;
		}
	}

	private static class OpIff extends BinaryOperator.Direct<Boolean> {
		private OpIff(String id) {
			super(id, PRIORITY_COMPARE);
		}

		@Override
		public Boolean execute(Boolean left, Boolean right) {
			return left == right;
		}
	}

	private static class OpXor extends BinaryOperator.Direct<Boolean> {
		private OpXor(String id) {
			super(id, PRIORITY_COMPARE);
		}

		@Override
		public Boolean execute(Boolean left, Boolean right) {
			return left ^ right;
		}
	}

	private static class OpNot extends UnaryOperator.Direct<Boolean> {
		private OpNot(String id) {
			super(id);
		}

		@Override
		public Boolean execute(Boolean value) {
			return !value;
		}
	}

	@Override
	protected void configureOperators(OperatorDictionary<Boolean> operators) {
		operators.registerUnaryOperator(new OpNot("~"));
		operators.registerUnaryOperator(new OpNot("not"));

		operators.registerBinaryOperator(new OpXor("^"));
		operators.registerBinaryOperator(new OpXor("xor"));
		operators.registerBinaryOperator(new OpXor("!="));

		operators.registerBinaryOperator(new OpIff("="));
		operators.registerBinaryOperator(new OpIff("<=>"));
		operators.registerBinaryOperator(new OpIff("eq"));
		operators.registerBinaryOperator(new OpIff("iff"));

		operators.registerBinaryOperator(new OpImplies("=>"));
		operators.registerBinaryOperator(new OpImplies("implies"));

		operators.registerBinaryOperator(new OpOr("|"));
		operators.registerBinaryOperator(new OpOr("or"));

		operators.registerBinaryOperator(new OpAnd("&"));
		operators.registerBinaryOperator(new OpAnd("and"));
	}

	public static Calculator<Boolean, ExprType> createSimple() {
		return new BoolCalculatorFactory<ExprType>().create(new BasicCompilerMapFactory<Boolean>());
	}

	public static Calculator<Boolean, ExprType> createDefault() {
		final CommonSimpleSymbolFactory<Boolean> letFactory = new CommonSimpleSymbolFactory<Boolean>(":", PRIORITY_COLON);

		return new BoolCalculatorFactory<ExprType>() {
			@Override
			protected void configureOperators(OperatorDictionary<Boolean> operators) {
				super.configureOperators(operators);
				operators.registerBinaryOperator(letFactory.getKeyValueSeparator());
			}
		}.create(letFactory.createCompilerFactory());
	}

}
