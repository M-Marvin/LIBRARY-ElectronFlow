package tvnlnna.mathematical;

public class MathematicalEvaluationException extends RuntimeException {
	
	private static final long serialVersionUID = -4872070979540013337L;
	
	public MathematicalEvaluationException(String msg) {
		super(msg);
	}

	public MathematicalEvaluationException(Throwable cause) {
		super(cause);
	}

	public MathematicalEvaluationException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
