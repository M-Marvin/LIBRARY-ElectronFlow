package tvnlnna.mathematical;

public class MathematicalExpressionException extends RuntimeException {
	
	private static final long serialVersionUID = -4872070979540013337L;
	
	public MathematicalExpressionException(String msg) {
		super(msg);
	}

	public MathematicalExpressionException(Throwable cause) {
		super(cause);
	}

	public MathematicalExpressionException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public MathematicalExpressionException(String expression, int position, String msg) {
		super(expression.substring(0, position) + " <- [HERE]: " + msg);
	}

	public MathematicalExpressionException(String expression, int position, String msg, Throwable cause) {
		super(expression.substring(0, position) + " <- [HERE]: " + msg, cause);
	}
	
}
