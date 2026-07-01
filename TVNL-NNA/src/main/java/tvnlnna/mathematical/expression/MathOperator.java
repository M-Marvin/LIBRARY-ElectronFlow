package tvnlnna.mathematical.expression;

/**
 * An operator is an special type of a {@linkplain MathTerm} which has additional properties for precedence resolution during parsing.
 */
public interface MathOperator extends MathTerm {
	
	/**
	 * The precedence of this operator, determines the order in which operators in the infix notation are resolved, and thus the order of which they end up after the postfix conversion.
	 * @return An integer representing the precedence value, larger values mean higher precedence
	 */
	public default int precedence() {
		return 0;
	};

	/**
	 * Weather the operator is left or right associative, determines the order in which operators of the same precedence in the infix notation are resolved, and thus the order of which they end up after the postfix conversion.
	 * @return true if the operator is left associative, false if its right associative
	 */
	public default boolean leftAssociative() {
		return true;
	}
	
	@Override
	public default TermType type() {
		return TermType.OPERATOR;
	}

	/**
	 * Weather this operator acts as an implicit opening bracket.<br>
	 * An implicit bracket acts like an normal bracket, except that it does not need to be closed explicitly, it is automatically closed at the end of the expression or when a surrounding bracket is closed.<br>
	 * Implicit brackets are used when the operator is followed by more than one argument, to allow separators to be used.
	 * @return
	 */
	public default boolean implicitBracket() {
		return false;
	}
	
}
