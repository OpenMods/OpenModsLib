package openmods.calc.command;

import java.math.BigInteger;
import java.util.*;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import openmods.calc.*;
import openmods.calc.Calculator.ExprType;
import openmods.config.simpler.ConfigurableClassAdapter;
import openmods.utils.Stack;

import org.apache.commons.lang3.math.Fraction;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CalcState {

	public static class NoSuchNameException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public NoSuchNameException(String message) {
			super(message);
		}
	}

	private static interface IFunction<E> {
		public E call();
	}

	private static class SenderHolder {
		private ICommandSender sender;

		public int getX() {
			Preconditions.checkNotNull(sender, "DERP");
			return sender.getPlayerCoordinates().posX;
		}

		public int getY() {
			Preconditions.checkNotNull(sender, "DERP");
			return sender.getPlayerCoordinates().posY;
		}

		public int getZ() {
			Preconditions.checkNotNull(sender, "DERP");
			return sender.getPlayerCoordinates().posZ;
		}

		public <E> Calculator<E> addPrinter(final Calculator<E> calculator) {
			calculator.setGlobalSymbol("p", new ISymbol<E>() {
				@Override
				public void execute(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
					Preconditions.checkNotNull(sender, "DERP");

					if (returnsCount.isPresent() && returnsCount.get() != 0) throw new StackValidationException("This function does not return any values");

					final Stack<E> stack = frame.stack();
					final int in = argumentsCount.or(1);

					final List<String> results = Lists.newArrayListWithExpectedSize(in);
					for (int i = 0; i < in; i++) {
						final E value = stack.pop();
						results.add(calculator.toString(value));
					}

					final String result = ": " + Joiner.on(" ").join(results);
					sender.addChatMessage(new ChatComponentText(result));
				}
			});
			return calculator;
		}

		public <E> E call(ICommandSender sender, IFunction<E> function) {
			this.sender = sender;
			final E result = function.call();
			this.sender = null;
			return result;
		}
	}

	public enum CalculatorType {
		DOUBLE {
			@Override
			public Calculator<?> newCalculator(final SenderHolder holder) {
				final DoubleCalculator calculator = new DoubleCalculator();

				calculator.setGlobalSymbol("$x", new FixedSymbol<Double>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<Double> frame) {
						frame.stack().push(Double.valueOf(holder.getX()));
					}
				});

				calculator.setGlobalSymbol("$y", new FixedSymbol<Double>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<Double> frame) {
						frame.stack().push(Double.valueOf(holder.getY()));
					}
				});

				calculator.setGlobalSymbol("$z", new FixedSymbol<Double>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<Double> frame) {
						frame.stack().push(Double.valueOf(holder.getZ()));
					}
				});

				return calculator;
			}
		},
		FRACTION {
			@Override
			public Calculator<?> newCalculator(final SenderHolder holder) {
				final FractionCalculator calculator = new FractionCalculator();

				calculator.setGlobalSymbol("$x", new FixedSymbol<Fraction>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<Fraction> frame) {
						frame.stack().push(Fraction.getFraction(holder.getX(), 1));
					}
				});

				calculator.setGlobalSymbol("$y", new FixedSymbol<Fraction>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<Fraction> frame) {
						frame.stack().push(Fraction.getFraction(holder.getY(), 1));
					}
				});

				calculator.setGlobalSymbol("$z", new FixedSymbol<Fraction>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<Fraction> frame) {
						frame.stack().push(Fraction.getFraction(holder.getZ(), 1));
					}
				});

				return calculator;
			}
		},
		BIGINT {
			@Override
			public Calculator<?> newCalculator(final SenderHolder holder) {
				final BigIntCalculator calculator = new BigIntCalculator();

				calculator.setGlobalSymbol("$x", new FixedSymbol<BigInteger>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<BigInteger> frame) {
						frame.stack().push(BigInteger.valueOf(holder.getX()));
					}
				});

				calculator.setGlobalSymbol("$y", new FixedSymbol<BigInteger>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<BigInteger> frame) {
						frame.stack().push(BigInteger.valueOf(holder.getY()));
					}
				});

				calculator.setGlobalSymbol("$z", new FixedSymbol<BigInteger>(0, 1) {
					@Override
					public void execute(ICalculatorFrame<BigInteger> frame) {
						frame.stack().push(BigInteger.valueOf(holder.getZ()));
					}
				});

				return calculator;
			}
		};

		public Calculator<?> createCalculator(SenderHolder holder) {
			return holder.addPrinter(newCalculator(holder));
		}

		protected abstract Calculator<?> newCalculator(SenderHolder holder);
	}

	private final SenderHolder senderHolder = new SenderHolder();

	private Calculator<?> active = CalculatorType.DOUBLE.createCalculator(senderHolder);

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
		setActiveCalculator(type.createCalculator(senderHolder));
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

	public void compileAndExecute(ICommandSender sender, final String expr) {
		senderHolder.call(sender, new IFunction<Void>() {
			@Override
			public Void call() {
				compileAndExecute(active, exprType, expr);
				return null;
			}
		});
	}

	private static <E> E compileExecuteAndPop(Calculator<E> calculator, Calculator.ExprType exprType, String expr) {
		final IExecutable<E> executable = calculator.compile(exprType, expr);
		return calculator.executeAndPop(executable);
	}

	private static <E> String compileExecuteAndPrint(Calculator<E> calculator, Calculator.ExprType exprType, String expr) {
		final E result = compileExecuteAndPop(calculator, exprType, expr);
		return calculator.toString(result);
	}

	public String compileExecuteAndPrint(ICommandSender sender, final String expr) {
		return senderHolder.call(sender, new IFunction<String>() {
			@Override
			public String call() {
				return compileExecuteAndPrint(active, exprType, expr);
			}
		});
	}

	private static <E> E compileAndSetGlobalSymbol(Calculator<E> calculator, Calculator.ExprType exprType, String id, String expr) {
		final E value = compileExecuteAndPop(calculator, exprType, expr);
		calculator.setGlobalSymbol(id, Constant.create(value));
		return value;
	}

	public Object compileAndSetGlobalSymbol(ICommandSender sender, final String id, final String expr) {
		return senderHolder.call(sender, new IFunction<Object>() {
			@Override
			public Object call() {
				return compileAndSetGlobalSymbol(active, exprType, id, expr);
			}
		});
	}

	// TODO: multiple return functions?
	private static <E> void compileAndDefineGlobalFunction(Calculator<E> calculator, Calculator.ExprType exprType, String id, int argCount, String bodyExpr) {
		final IExecutable<E> funcBody = calculator.compile(exprType, bodyExpr);
		calculator.setGlobalSymbol(id, new CompiledFunction<E>(argCount, 1, funcBody));
	}

	public void compileAndDefineGlobalFunction(ICommandSender sender, final String id, final int argCount, final String expr) {
		senderHolder.call(sender, new IFunction<Void>() {
			@Override
			public Void call() {
				compileAndDefineGlobalFunction(active, exprType, id, argCount, expr);
				return null;
			}
		});

	}

}
