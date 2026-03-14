package de.m_marvin.electronflow.nltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.nltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Voltage2Current extends FourPort {
	
	protected double factor;
	
	public Voltage2Current(String name, String nodeA, String nodeB, String nodeC, String nodeD, double factor) {
		super(name, nodeA, nodeB, nodeC, nodeD);
		this.factor = factor;
	}
	
	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("VUI") && args.length == 6) {
			double value = DecimalPrefixFormater.parseDouble(args[5]);
			return Optional.of(new Voltage2Current(args[0], args[1], args[2], args[3], args[4], value));
			
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "VUI";
	}
	
	@Override
	public String configInfo() {
		return DecimalPrefixFormater.format("%s", this.factor);
	}
	
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	public double factor() {
		return factor;
	}
	
	@Override
	public double[] currents() {
		double p0 = this.network.getNodePotential(this.nodeAid);
		double p1 = this.network.getNodePotential(this.nodeBid);
		return new double[] {
				(p1 - p0) * this.factor
		};
	}

	@Override
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {

		if (ctx.stampsA()) {
			if (this.nodeAid != 0 && this.nodeCid != 0)
				A.addM(this.nodeAid - 1, this.nodeCid - 1, +this.factor);
			if (this.nodeBid != 0 && this.nodeDid != 0)
				A.addM(this.nodeBid - 1, this.nodeDid - 1, +this.factor);
			if (this.nodeAid != 0 && this.nodeDid != 0)
				A.addM(this.nodeAid - 1, this.nodeBid - 1, -this.factor);
			if (this.nodeBid != 0 && this.nodeCid != 0)
				A.addM(this.nodeBid - 1, this.nodeCid - 1, -this.factor);
		}
		
	}
	
}
