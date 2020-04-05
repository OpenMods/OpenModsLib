package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import info.openmods.calc.Calculator;
import info.openmods.calc.ExprType;
import info.openmods.calc.IValuePrinter;
import info.openmods.calc.symbol.NullaryFunction;
import info.openmods.calc.symbol.UnaryFunction;
import info.openmods.calc.types.bigint.BigIntCalculatorFactory;
import info.openmods.calc.types.bool.BoolCalculatorFactory;
import info.openmods.calc.types.fp.DoubleCalculatorFactory;
import info.openmods.calc.types.fraction.FractionCalculatorFactory;
import info.openmods.calc.types.multi.StructWrapper;
import info.openmods.calc.types.multi.TypeDomain;
import info.openmods.calc.types.multi.TypedValue;
import info.openmods.calc.types.multi.TypedValueCalculatorFactory;
import info.openmods.calc.utils.Stack;
import info.openmods.calc.utils.StackValidationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.math.Fraction;

public class CalcState {

	public static class NoSuchNameException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public NoSuchNameException(String message) {
			super(message);
		}
	}

	private interface IFunction<E> {
		E call();
	}

	private static class SenderHolder {
		private CommandSource sender;

		public double getX() {
			Preconditions.checkNotNull(sender, "DERP");
			return sender.getPos().getX();
		}

		public double getY() {
			Preconditions.checkNotNull(sender, "DERP");
			return sender.getPos().getY();
		}

		public double getZ() {
			Preconditions.checkNotNull(sender, "DERP");
			return sender.getPos().getZ();
		}

		public <E> Calculator<E, ExprType> addPrinter(Calculator<E, ExprType> calculator) {
			final IValuePrinter<E> printer = calculator.printer;
			calculator.environment.setGlobalSymbol("p", (frame, argumentsCount, returnsCount) -> {
				Preconditions.checkNotNull(sender, "DERP");

				if (!returnsCount.compareIfPresent(0)) throw new StackValidationException("This function does not return any values");

				final Stack<E> stack = frame.stack();
				final int in = argumentsCount.or(1);

				final List<String> results = Lists.newArrayListWithExpectedSize(in);
				for (int i = 0; i < in; i++) {
					final E value = stack.pop();
					results.add(printer.repr(value));
				}

				final String result = ": " + Joiner.on(" ").join(results);
				sender.sendFeedback(new StringTextComponent(result), false);
			});

			calculator.environment.setGlobalSymbol("print", new UnaryFunction.Direct<E>() {
				@Override
				protected E call(E value) {
					sender.sendFeedback(new StringTextComponent(printer.str(value)), false);
					return value;
				}
			});
			return calculator;
		}

		public synchronized <E> E call(CommandSource sender, IFunction<E> function) {
			this.sender = sender;
			final E result = function.call();
			this.sender = null;
			return result;
		}
	}

	public enum CalculatorType {
		DOUBLE {
			@Override
			public Calculator<?, ExprType> newCalculator(final SenderHolder holder) {
				final Calculator<Double, ExprType> calculator = DoubleCalculatorFactory.createDefault();

				calculator.environment.setGlobalSymbol("_x", holder::getX);

				calculator.environment.setGlobalSymbol("_y", holder::getY);

				calculator.environment.setGlobalSymbol("_z", holder::getZ);

				return calculator;
			}
		},
		FRACTION {
			@Override
			public Calculator<?, ExprType> newCalculator(final SenderHolder holder) {
				final Calculator<Fraction, ExprType> calculator = FractionCalculatorFactory.createDefault();

				calculator.environment.setGlobalSymbol("_x", () -> Fraction.getFraction(holder.getX()));

				calculator.environment.setGlobalSymbol("_y", () -> Fraction.getFraction(holder.getY()));

				calculator.environment.setGlobalSymbol("_z", () -> Fraction.getFraction(holder.getZ()));

				return calculator;
			}
		},
		BIGINT {
			@Override
			public Calculator<?, ExprType> newCalculator(final SenderHolder holder) {
				final Calculator<BigInteger, ExprType> calculator = BigIntCalculatorFactory.createDefault();

				calculator.environment.setGlobalSymbol("_x", () -> BigDecimal.valueOf(holder.getX()).toBigInteger());

				calculator.environment.setGlobalSymbol("_y", () -> BigDecimal.valueOf(holder.getY()).toBigInteger());

				calculator.environment.setGlobalSymbol("_z", () -> BigDecimal.valueOf(holder.getZ()).toBigInteger());

				return calculator;
			}
		},
		MULTI {
			@Override
			protected Calculator<?, ExprType> newCalculator(final SenderHolder holder) {
				final Calculator<TypedValue, ExprType> calculator = TypedValueCalculatorFactory.create();

				final TypedValue nullValue = calculator.environment.nullValue();
				final TypeDomain domain = nullValue.domain;
				calculator.environment.setGlobalSymbol("player", new NullaryFunction.Direct<TypedValue>() {

					@Override
					protected TypedValue call() {
						if (holder.sender.getEntity() instanceof PlayerEntity) {
							final EntityPlayerWrapper wrapper = new EntityPlayerWrapper((PlayerEntity)holder.sender.getEntity(), nullValue);
							return StructWrapper.create(domain, wrapper);
						}

						return nullValue;
					}

				});

				return calculator;
			}
		},
		BOOL {
			@Override
			protected Calculator<?, ExprType> newCalculator(SenderHolder holder) {
				return BoolCalculatorFactory.createDefault();
			}
		};

		public Calculator<?, ExprType> createCalculator(SenderHolder holder) {
			return holder.addPrinter(newCalculator(holder));
		}

		protected abstract Calculator<?, ExprType> newCalculator(SenderHolder holder);
	}

	private final SenderHolder senderHolder = new SenderHolder();

	private Calculator<?, ExprType> active = CalculatorType.DOUBLE.createCalculator(senderHolder);

	private Calculator<?, ExprType> prev;

	public ExprType exprType = ExprType.INFIX;

	private final Stack<Calculator<?, ExprType>> calculatorStack = Stack.create();

	private final Map<String, Calculator<?, ExprType>> calculatorMap = Maps.newHashMap();

	public Calculator<?, ExprType> getActiveCalculator() {
		return active;
	}

	private void setActiveCalculator(final Calculator<?, ExprType> newCalculator) {
		prev = active;
		active = newCalculator;
	}

	public void restorePreviousCalculator() {
		final Calculator<?, ExprType> tmp = active;
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
		final Calculator<?, ExprType> newCalculator = calculatorMap.get(name);
		if (newCalculator == null) throw new NoSuchNameException(name);
		setActiveCalculator(newCalculator);
	}

	public void compileAndExecute(CommandSource sender, final String expr) {
		senderHolder.call(sender, () -> {
			active.compileAndExecute(exprType, expr);
			return null;
		});
	}

	public String compileExecuteAndPrint(CommandSource sender, final String expr) {
		return senderHolder.call(sender, () -> active.compileExecuteAndPrint(exprType, expr));
	}

	public Object compileAndSetGlobalSymbol(CommandSource sender, final String id, final String expr) {
		return senderHolder.call(sender, () -> active.compileAndSetGlobalSymbol(exprType, id, expr));
	}

	public void compileAndDefineGlobalFunction(CommandSource sender, final String id, final int argCount, final String expr) {
		senderHolder.call(sender, () -> {
			active.compileAndDefineGlobalFunction(exprType, id, argCount, expr);
			return null;
		});

	}

}
