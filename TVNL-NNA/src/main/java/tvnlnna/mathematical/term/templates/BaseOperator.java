package tvnlnna.mathematical.term.templates;

import tvnlnna.mathematical.expression.MathOperator;

public abstract class BaseOperator implements MathOperator {
	
	private final String symbol;
	private final int precedence;
	private final boolean leftassociative;
	
	public BaseOperator(String symbol, int precedence, boolean leftassociative) {
		this.symbol = symbol;
		this.precedence = precedence;
		this.leftassociative = leftassociative;
	}
	
	@Override
	public int precedence() {
		return this.precedence;
	}
	
	@Override
	public boolean leftAssociative() {
		return this.leftassociative;
	}
	
	@Override
	public String str() {
		return this.symbol;
	}

	@Override
	public String toString() {
		return "BaseOperator{ " + str() + " }";
	}
	
}
