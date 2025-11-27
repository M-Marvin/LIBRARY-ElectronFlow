package test.esim.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.unimat.impl.MatrixNd;
import test.esim.Component;
import test.esim.Network.Context;

public class Capacitor extends TwoPort {

	protected double capacitance;
	
	public Capacitor(String id, int nodeA, int nodeB, double capacitance) {
		super(id, nodeA, nodeB);
		this.capacitance = capacitance;
	}

	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("C") && args.length == 4) {
			double value = Double.parseDouble(args[3]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			return Optional.of(new Capacitor(args[0], nodeA, nodeB, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "C";
	}
	
	@Override
	public String configInfo() {
		return String.format("%.03f F", this.capacitance);
	}
	
	public void setCapacitance(double capacitance) {
		this.capacitance = capacitance;
	}
	
	public double capacitance() {
		return capacitance;
	}
	
	@Override
	public void stampMatricies(Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {
		
		if (this.nodeA != 0)
			E.addM(this.nodeA - 1, this.nodeA - 1, +this.capacitance);
		if (this.nodeB != 0)
			E.addM(this.nodeB - 1, this.nodeB - 1, +this.capacitance);
		if (this.nodeA != 0 && this.nodeB != 0) {
			E.addM(this.nodeA - 1, this.nodeB - 1, -this.capacitance);
			E.addM(this.nodeB - 1, this.nodeA - 1, -this.capacitance);
		}
		
	}
	
}
