package tvnlnna.mathematical.expression.misc;

/**
 * Helper interface to define the derivative of dual mathematical operators
 */
@FunctionalInterface
public interface DualOperationDiff {
	public double apply(double B, double Bd, double A, double Ad);
}
