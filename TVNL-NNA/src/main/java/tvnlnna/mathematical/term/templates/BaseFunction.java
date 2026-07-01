package tvnlnna.mathematical.term.templates;

import tvnlnna.mathematical.expression.MathTerm;

public abstract class BaseFunction implements MathTerm {

	private final String name;
	
	public BaseFunction(String name) {
		this.name = name;
	}
	
	@Override
	public String str() {
		return name;
	}

	@Override
	public TermType type() {
		return TermType.LABEL;
	}
	
	@Override
	public String toString() {
		return "BaseFunction{ " + str() + " }";
	}
	
}
