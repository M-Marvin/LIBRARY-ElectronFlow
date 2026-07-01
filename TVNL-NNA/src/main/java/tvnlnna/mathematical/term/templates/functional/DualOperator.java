package tvnlnna.mathematical.term.templates.functional;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;

import tvnlnna.mathematical.MathematicalEvaluationException;
import tvnlnna.mathematical.expression.misc.DualOperation;
import tvnlnna.mathematical.expression.misc.DualOperationDiff;

public class DualOperator extends BaseOperator {
	
	private final DualOperation operation;
	private final DualOperationDiff differentiation;
	
	public DualOperator(String symbol, int precedence, boolean leftassoative, DualOperation operation, DualOperationDiff differentiation) {
		super(symbol, precedence, leftassoative);
		this.operation = operation;
		this.differentiation = differentiation;
	}
	
	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			evalstack.add(this.operation.apply(evalstack.pop(),  evalstack.pop()));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
		}
	}
	
	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		try {
			double B = evalstack.pop();
			double A = evalstack.pop();
			evalstack.add(this.operation.apply(B, A));
			if (this.differentiation != null)
				diffstack.add(this.differentiation.apply(B, diffstack.pop(), A, diffstack.pop()));
			else
				diffstack.add(evalstack.peek());
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for operation: " + str());
		}
	}
	
}