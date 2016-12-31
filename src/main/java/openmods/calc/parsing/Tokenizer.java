package openmods.calc.parsing;

import com.google.common.collect.Sets;
import java.util.Set;

public class Tokenizer {

	final Set<String> operators = Sets.newHashSet();

	final Set<String> modifiers = Sets.newHashSet();

	public void addOperator(String operator) {
		operators.add(operator);
	}

	public void addModifier(String special) {
		modifiers.add(special);
	}

	public TokenIterator tokenize(String input) {
		return new TokenIterator(input, operators, modifiers);
	}
}
