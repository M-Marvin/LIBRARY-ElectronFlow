package de.m_marvin.electronflow.ltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork;
import de.m_marvin.unimat.impl.MatrixNd;

public class Resistor extends TwoPort {
	
	protected double resistance;
	
	public Resistor(String name, String nodeA, String nodeB, double resistance) {
		super(name, nodeA, nodeB);
		this.resistance = resistance;
	}

	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("R") && args.length == 4) {
			double value = Double.parseDouble(args[3]);
			return Optional.of(new Resistor(args[0], args[1], args[2], value));
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
	public double[] currents() {
		double p0 = this.network.getNodePotential(this.nodeAid);
		double p1 = this.network.getNodePotential(this.nodeBid);
		return new double[] {
				(p1 - p0) / this.resistance
		};
	}
	
	@Override
	public void stampMatricies(IndexedNetwork.StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		if (ctx.stampsA()) {
			double conductance = 1.0 / this.resistance;
			if (this.nodeAid != 0)
				A.addM(this.nodeAid - 1, this.nodeAid - 1, +conductance);
			if (this.nodeBid != 0)
				A.addM(this.nodeBid - 1, this.nodeBid - 1, +conductance);
			if (this.nodeAid != 0 && this.nodeBid != 0) {
				A.addM(this.nodeAid - 1, this.nodeBid - 1, -conductance);
				A.addM(this.nodeBid - 1, this.nodeAid - 1, -conductance);
			}
		}
		
	}

}
