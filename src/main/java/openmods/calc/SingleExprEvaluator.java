package openmods.calc;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import openmods.Log;
import openmods.calc.executable.IExecutable;

public class SingleExprEvaluator<E, M> {

	public interface EnvironmentConfigurator<E> {
		public void accept(Environment<E> env);
	}

	private final Calculator<E, M> calculator;

	private boolean useFallback;

	private M exprType;

	private String expr;

	private IExecutable<E> compiledExpr;

	public SingleExprEvaluator(Calculator<E, M> calculator) {
		this.calculator = calculator;
	}

	public static <E, M> SingleExprEvaluator<E, M> create(Calculator<E, M> calculator) {
		return new SingleExprEvaluator<E, M>(calculator);
	}

	public void setExpr(M exprType, String expr) {
		this.exprType = exprType;
		this.expr = expr;
		this.compiledExpr = null;
		this.useFallback = false;
	}

	public E evaluate(EnvironmentConfigurator<E> conf, Supplier<E> fallbackValue) {
		if (useFallback || Strings.isNullOrEmpty(expr) || exprType == null) return fallbackValue.get();

		if (compiledExpr == null) {
			try {
				compiledExpr = calculator.compilers.compile(exprType, expr);
			} catch (Exception ex) {
				useFallback = true;
				Log.warn(ex, "Failed to compile formula %s", expr);
				return fallbackValue.get();
			}
		}

		conf.accept(calculator.environment);

		try {
			return calculator.environment.executeAndPop(compiledExpr);
		} catch (Exception ex) {
			useFallback = true;
			Log.warn(ex, "Failed to execute formula %s", expr);
			return fallbackValue.get();
		}
	}

}
