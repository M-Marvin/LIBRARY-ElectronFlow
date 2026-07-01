package tvnlnna.mathematical.expression.misc;

@FunctionalInterface
public interface SingleOperationDiff {
	public double apply(double x, double xd);
}
