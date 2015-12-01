package openmods.calc.command;

import java.util.Map;

import openmods.calc.*;
import openmods.calc.Calculator.ExprType;
import openmods.utils.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class CalcState {

	public enum CalculatorType {
		DOUBLE {
			@Override
			public Calculator<?> createCalculator() {
				return new DoubleCalculator();
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

	public int base = 10;

	private Stack<Calculator<?>> calculatorStack = Stack.create();

	private Map<String, Calculator<?>> calculatorMap = Maps.newHashMap();

	public Calculator<?> getActiveCalculator() {
		return active;
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

	public void pushCalculator() {
		calculatorStack.push(active);
	}

	public void popCalculator() {
		if (calculatorStack.isEmpty()) throw new IllegalStateException("Stack underflow");
		setActiveCalculator(calculatorStack.pop());
	}

	public void nameCalculator(String name) {
		calculatorMap.put(name, active);
	}

	public void loadCalculator(String name) {
		final Calculator<?> newCalculator = calculatorMap.get(name);
		Preconditions.checkState(newCalculator != null, "No calculator named %s", name);
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

	public Object compileExecuteAndPop(String expr) {
		return compileExecuteAndPop(active, exprType, expr);
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
