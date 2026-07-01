package tvnlnna.mathematical.term.templates.functional;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;

import tvnlnna.mathematical.MathematicalEvaluationException;
import tvnlnna.mathematical.expression.misc.SingleOperation;
import tvnlnna.mathematical.expression.misc.SingleOperationDiff;

public class SingleFunction extends BaseFunction {
	
	private final SingleOperation function;
	private final SingleOperationDiff differentiation;
	
	public SingleFunction(String name, SingleOperation function, SingleOperationDiff differentiation) {
		super(name);
		this.function = function;
		this.differentiation = differentiation;
	}

	@Override
	public void evaluate(Stack<Double> evalstack, Map<String, Double> parameters) {
		try {
			evalstack.add(this.function.apply(evalstack.pop()));
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}

	@Override
	public void evaluateAndDerive(Stack<Double> evalstack, Stack<Double> diffstack, Map<String, Double> parameters, String variable) {
		try {
			double x = evalstack.pop();
			evalstack.add(this.function.apply(x));
			if (this.differentiation != null)
				diffstack.add(this.differentiation.apply(x, diffstack.pop()));
			else
				diffstack.add(evalstack.peek());
		} catch (EmptyStackException e) {
			throw new MathematicalEvaluationException("lacking evalutation stack entries for function: " + str());
		}
	}
	
}