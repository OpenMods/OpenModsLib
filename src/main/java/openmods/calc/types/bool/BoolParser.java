package openmods.calc.types.bool;

import com.google.common.base.Preconditions;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.token.Token;

public class BoolParser implements IValueParser<Boolean> {

	@Override
	public Boolean parseToken(Token token) {
		Preconditions.checkArgument(token.type.isNumber(), "Not a number: %s", token);
		if (token.value.equals("1")) return Boolean.TRUE;
		if (token.value.equals("0")) return Boolean.FALSE;
		throw new IllegalArgumentException("Must one either 0 or 1, got: " + token.value);
	}

}
