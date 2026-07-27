package tvnlnna.mathematical.expression.misc;

/**
 * Helper interface to define single mathematical operators
 */
@FunctionalInterface
public interface SingleOperation {
	public double apply(double x);
}
