package openmods.calc.types.multi;

import openmods.calc.executable.IExecutable;
import openmods.calc.executable.Operator;
import openmods.calc.executable.Value;
import openmods.calc.parsing.SymbolGetPostfixCompilerState;
import openmods.calc.parsing.ast.IOperatorDictionary;
import openmods.calc.parsing.ast.OperatorArity;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.symbol.CallableOperatorWrapper;
import openmods.calc.symbol.ICallable;

public class CallableGetPostfixCompilerState extends SymbolGetPostfixCompilerState<TypedValue> {

	private final IOperatorDictionary<Operator<TypedValue>> operators;
	private final TypeDomain domain;

	public CallableGetPostfixCompilerState(IOperatorDictionary<Operator<TypedValue>> operators, TypeDomain domain) {
		this.operators = operators;
		this.domain = domain;
	}

	@Override
	protected IExecutable<TypedValue> parseToken(Token token) {
		if (token.type == TokenType.OPERATOR) {
			Operator<TypedValue> op = operators.getOperator(token.value, OperatorArity.BINARY);
			if (op == null) op = operators.getOperator(token.value, OperatorArity.UNARY);
			if (op == null) return rejectToken();
			return createGetter(new CallableOperatorWrapper(op));
		}

		return super.parseToken(token);
	}

	private IExecutable<TypedValue> createGetter(ICallable<TypedValue> wrapper) {
		return Value.create(CallableValue.wrap(domain, wrapper));
	}

}
