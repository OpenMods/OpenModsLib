package openmods.calc;

public class Token {
	public final TokenType type;
	public final String value;

	public Token(TokenType type, String value) {
		this.type = type;
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null)? 0 : type.hashCode());
		result = prime * result + ((value == null)? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;

		if (obj instanceof Token) {
			final Token other = (Token)obj;
			return other.type == type && other.value.equals(value);
		}

		return true;
	}

	@Override
	public String toString() {
		return "['" + value + "'->" + type + "]";
	}

}