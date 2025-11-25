package de.m_marvin.electronflow.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.electronflow.Component;
import de.m_marvin.electronflow.Network.Context;
import de.m_marvin.unimat.impl.MatrixNd;

public class Voltage2Current extends FourPort {
	
	protected double factor;
	
	public Voltage2Current(String name, int nodeA, int nodeB, int nodeC, int nodeD, double factor) {
		super(name, nodeA, nodeB, nodeC, nodeD);
		this.factor = factor;
	}
	
	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("VUI") && args.length == 6) {
			double value = Double.parseDouble(args[5]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			int nodeC = nodeIdProvider.apply(args[3]);
			int nodeD = nodeIdProvider.apply(args[4]);
			return Optional.of(new Voltage2Current(args[0], nodeA, nodeB, nodeC, nodeD, value));
			
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "VUI";
	}
	
	@Override
	public String configInfo() {
		return String.format("%.03f A/V", this.factor);
	}
	
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	public double factor() {
		return factor;
	}
	
	@Override
	public void stampMatricies(Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {
		
		if (this.nodeA != 0 && this.nodeC != 0)
			A.addM(this.nodeA - 1, this.nodeC - 1, +this.factor);
		if (this.nodeB != 0 && this.nodeD != 0)
			A.addM(this.nodeB - 1, this.nodeD - 1, +this.factor);
		if (this.nodeA != 0 && this.nodeD != 0)
			A.addM(this.nodeA - 1, this.nodeB - 1, -this.factor);
		if (this.nodeB != 0 && this.nodeC != 0)
			A.addM(this.nodeB - 1, this.nodeC - 1, -this.factor);
		
	}
	
}
