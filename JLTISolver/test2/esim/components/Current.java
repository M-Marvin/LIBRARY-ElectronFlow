package test.esim.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.unimat.impl.MatrixNd;
import test.esim.Component;
import test.esim.Network.Context;

public class Current extends TwoPort {
	
	protected double current;
	
	public Current(String name, int nodeA, int nodeB, double current) {
		super(name, nodeA, nodeB);
		this.current = current;
	}

	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("I") && args.length == 4) {
			double value = Double.parseDouble(args[3]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			return Optional.of(new Current(args[0], nodeA, nodeB, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "I";
	}
	
	@Override
	public String configInfo() {
		return String.format("%.03f A", this.current);
	}
	
	public void setCurrent(double current) {
		this.current = current;
	}
	
	public double current() {
		return current;
	}
	
	@Override
	public void stampMatricies(Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {
		
		z.addM(0, this.nodeA, -this.current);
		z.addM(0, this.nodeB, +this.current);
		
	}

}
