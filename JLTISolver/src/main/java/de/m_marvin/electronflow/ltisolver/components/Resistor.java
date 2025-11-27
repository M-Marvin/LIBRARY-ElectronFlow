package de.m_marvin.electronflow.ltisolver.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.electronflow.ltisolver.Component;
import de.m_marvin.electronflow.ltisolver.Network;
import de.m_marvin.unimat.impl.MatrixNd;

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
	public void stampMatricies(Network.StampingContext ctx, MatrixNd A, MatrixNd z) {

		if (!ctx.stampOnlyZ()) {
			double conductance = 1.0 / this.resistance;
			if (this.nodeA != 0)
				A.addM(this.nodeA - 1, this.nodeA - 1, +conductance);
			if (this.nodeB != 0)
				A.addM(this.nodeB - 1, this.nodeB - 1, +conductance);
			if (this.nodeA != 0 && this.nodeB != 0) {
				A.addM(this.nodeA - 1, this.nodeB - 1, -conductance);
				A.addM(this.nodeB - 1, this.nodeA - 1, -conductance);
			}
		}
		
	}

}
