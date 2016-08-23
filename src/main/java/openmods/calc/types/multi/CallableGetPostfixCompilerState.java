package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.BinaryOperator;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryOperator;
import openmods.calc.Value;
import openmods.calc.parsing.CallableOperatorWrappers;
import openmods.calc.parsing.SymbolGetPostfixCompilerState;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;

public class CallableGetPostfixCompilerState extends SymbolGetPostfixCompilerState<TypedValue> {

	private final OperatorDictionary<TypedValue> operators;
	private final TypeDomain domain;

	public CallableGetPostfixCompilerState(OperatorDictionary<TypedValue> operators, TypeDomain domain) {
		this.operators = operators;
		this.domain = domain;
	}

	@Override
	protected Optional<? extends IExecutable<TypedValue>> parseToken(Token token) {
		if (token.type == TokenType.OPERATOR) {
			final BinaryOperator<TypedValue> binaryOp = operators.getBinaryOperator(token.value);
			if (binaryOp != null) return createGetter(new CallableOperatorWrappers.Binary(binaryOp));

			final UnaryOperator<TypedValue> unaryOp = operators.getUnaryOperator(token.value);
			if (unaryOp != null) return createGetter(new CallableOperatorWrappers.Unary(unaryOp));

			throw new IllegalArgumentException("Unknown operator: " + token.value);
		}

		return super.parseToken(token);
	}

	private Optional<Value<TypedValue>> createGetter(final ICallable<TypedValue> wrapper) {
		return Optional.of(Value.create(domain.create(ICallable.class, wrapper)));
	}

}
