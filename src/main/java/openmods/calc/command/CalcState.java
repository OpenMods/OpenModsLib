package openmods.calc.command;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import openmods.calc.*;
import openmods.calc.Calculator.ExprType;
import openmods.config.simpler.ConfigurableClassAdapter;
import openmods.utils.Stack;

import com.google.common.collect.Maps;

public class CalcState {

	public static class NoSuchNameException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public NoSuchNameException(String message) {
			super(message);
		}
	}

	public enum CalculatorType {
		DOUBLE {
			@Override
			public Calculator<?> createCalculator() {
				return new DoubleCalculator();
			}
		},
		FRACTION {
			@Override
			public Calculator<?> createCalculator() {
				return new FractionCalculator();
			}
		},
		BIGINT {
			@Override
			public Calculator<?> createCalculator() {
				return new BigIntCalculator();
			}
		};

		public abstract Calculator<?> createCalculator();
	}

	private Calculator<?> active = new DoubleCalculator();

	private Calculator<?> prev;

	public Calculator.ExprType exprType = ExprType.INFIX;

	private Stack<Calculator<?>> calculatorStack = Stack.create();

	private Map<String, Calculator<?>> calculatorMap = Maps.newHashMap();

	public Calculator<?> getActiveCalculator() {
		return active;
	}

	public Set<String> getActiveProperties() {
		return ConfigurableClassAdapter.getFor(active.getClass()).keys();
	}

	public String getActiveProperty(String key) {
		return ConfigurableClassAdapter.getFor(active.getClass()).get(active, key);
	}

	public void setActiveProperty(String key, String value) {
		ConfigurableClassAdapter.getFor(active.getClass()).set(active, key, value);
	}

	private void setActiveCalculator(final Calculator<?> newCalculator) {
		prev = active;
		active = newCalculator;
	}

	public void restorePreviousCalculator() {
		final Calculator<?> tmp = active;
		active = prev;
		prev = tmp;
	}

	public void createCalculator(CalculatorType type) {
		setActiveCalculator(type.createCalculator());
	}

	public int pushCalculator() {
		calculatorStack.push(active);
		return calculatorStack.size();
	}

	public int popCalculator() {
		setActiveCalculator(calculatorStack.pop());
		return calculatorStack.size();
	}

	public void nameCalculator(String name) {
		calculatorMap.put(name, active);
	}

	public Set<String> getCalculatorsNames() {
		return Collections.unmodifiableSet(calculatorMap.keySet());
	}

	public void loadCalculator(String name) {
		final Calculator<?> newCalculator = calculatorMap.get(name);
		if (newCalculator == null) throw new NoSuchNameException(name);
		setActiveCalculator(newCalculator);
	}

	private static <E> void compileAndExecute(Calculator<E> calculator, Calculator.ExprType exprType, String expr) {
		final IExecutable<E> executable = calculator.compile(exprType, expr);
		calculator.execute(executable);
	}

	public void compileAndExecute(String expr) {
		compileAndExecute(active, exprType, expr);
	}

	private static <E> E compileExecuteAndPop(Calculator<E> calculator, Calculator.ExprType exprType, String expr) {
		final IExecutable<E> executable = calculator.compile(exprType, expr);
		return calculator.executeAndPop(executable);
	}

	private static <E> String compileExecuteAndPrint(Calculator<E> calculator, Calculator.ExprType exprType, String expr) {
		final E result = compileExecuteAndPop(calculator, exprType, expr);
		return calculator.toString(result);
	}

	public String compileExecuteAndPrint(String expr) {
		return compileExecuteAndPrint(active, exprType, expr);
	}

	private static <E> E compileAndSetGlobalSymbol(Calculator<E> calculator, Calculator.ExprType exprType, String id, String expr) {
		final E value = compileExecuteAndPop(calculator, exprType, expr);
		calculator.setGlobalSymbol(id, Constant.create(value));
		return value;
	}

	public Object compileAndSetGlobalSymbol(String id, String expr) {
		return compileAndSetGlobalSymbol(active, exprType, id, expr);
	}

	// TODO: multiple return functions?
	private static <E> void compileAndDefineGlobalFunction(Calculator<E> calculator, Calculator.ExprType exprType, String id, int argCount, String bodyExpr) {
		final IExecutable<E> funcBody = calculator.compile(exprType, bodyExpr);
		calculator.setGlobalSymbol(id, new CompiledFunction<E>(argCount, 1, funcBody));
	}

	public void compileAndDefineGlobalFunction(String id, int argCount, String expr) {
		compileAndDefineGlobalFunction(active, exprType, id, argCount, expr);
	}

}
