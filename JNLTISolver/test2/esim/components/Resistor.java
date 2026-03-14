package test.esim.elements;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.unimat.impl.MatrixNd;
import test.esim.Component;
import test.esim.Network;

public class Resistor extends TwoPort {
	
	protected double resistance;
	
	public Resistor(String name, int nodeA, int nodeB, double resistance) {
		super(name, nodeA, nodeB);
		this.resistance = resistance;
	}

	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("R") && args.length == 4) {
			double value = Double.parseDouble(args[3]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			return Optional.of(new Resistor(args[0], nodeA, nodeB, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "R";
	}

	@Override
	public String configInfo() {
		return String.format("%.03f R", this.resistance);
	}
	
	public void setResistance(double resistance) {
		this.resistance = resistance;
	}
	
	public double resistance() {
		return resistance;
	}
	
	@Override
	public void stampMatricies(Network.Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {
		
		double conductance = 1.0 / this.resistance;
		A.addM(this.nodeA, this.nodeA, +conductance);
		A.addM(this.nodeB, this.nodeB, +conductance);
		A.addM(this.nodeA, this.nodeB, -conductance);
		A.addM(this.nodeB, this.nodeA, -conductance);
		
	}

}
