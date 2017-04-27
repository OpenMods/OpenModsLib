package openmods.calc.types.bool;

import java.util.Random;
import openmods.calc.Calculator;
import openmods.calc.Environment;
import openmods.calc.ExprType;
import openmods.calc.IValuePrinter;
import openmods.calc.SimpleCalculatorFactory;
import openmods.calc.executable.BinaryOperator;
import openmods.calc.executable.Operator;
import openmods.calc.executable.OperatorDictionary;
import openmods.calc.executable.UnaryOperator;
import openmods.calc.parsing.BasicCompilerMapFactory;
import openmods.calc.parsing.CommonSimpleSymbolFactory;
import openmods.calc.parsing.IValueParser;
import openmods.calc.symbol.NullaryFunction;

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

		final Random random = new Random();

		env.setGlobalSymbol("rand", new NullaryFunction.Direct<Boolean>() {
			@Override
			protected Boolean call() {
				return random.nextBoolean();
			}
		});
	}

	private static final int PRIORITY_AND = 4; // &
	private static final int PRIORITY_OR = 3; // |
	private static final int PRIORITY_COMPARE = 2; // ^, =, =>
	private static final int PRIORITY_ASSIGN = 1;

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
	protected void configureOperators(OperatorDictionary<Operator<Boolean>> operators) {
		operators.registerOperator(new OpNot("~"));
		operators.registerOperator(new OpNot("not"));

		operators.registerOperator(new OpXor("^"));
		operators.registerOperator(new OpXor("xor"));
		operators.registerOperator(new OpXor("!="));

		operators.registerOperator(new OpIff("="));
		operators.registerOperator(new OpIff("<=>"));
		operators.registerOperator(new OpIff("eq"));
		operators.registerOperator(new OpIff("iff"));

		operators.registerOperator(new OpImplies("=>"));
		operators.registerOperator(new OpImplies("implies"));

		operators.registerOperator(new OpOr("|"));
		operators.registerOperator(new OpOr("or"));

		operators.registerOperator(new OpAnd("&"));
		operators.registerOperator(new OpAnd("and"));
	}

	public static Calculator<Boolean, ExprType> createSimple() {
		return new BoolCalculatorFactory<ExprType>().create(new BasicCompilerMapFactory<Boolean>());
	}

	public static Calculator<Boolean, ExprType> createDefault() {
		final CommonSimpleSymbolFactory<Boolean> letFactory = new CommonSimpleSymbolFactory<Boolean>(PRIORITY_ASSIGN, ":");

		return new BoolCalculatorFactory<ExprType>() {
			@Override
			protected void configureOperators(OperatorDictionary<Operator<Boolean>> operators) {
				super.configureOperators(operators);
				letFactory.registerSeparators(operators);
			}
		}.create(letFactory.createCompilerFactory());
	}

}
