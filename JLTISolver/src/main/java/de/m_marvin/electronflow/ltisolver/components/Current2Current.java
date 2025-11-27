package de.m_marvin.electronflow.ltisolver.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.electronflow.ltisolver.Component;
import de.m_marvin.electronflow.ltisolver.Network.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Current2Current extends FourPort {
	
	protected double factor;
	protected int vid;
	
	public Current2Current(String name, int nodeA, int nodeB, int nodeC, int nodeD, double factor) {
		super(name, nodeA, nodeB, nodeC, nodeD);
		this.factor = factor;
	}
	
	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("VII") && args.length == 6) {
			double value = Double.parseDouble(args[5]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			int nodeC = nodeIdProvider.apply(args[3]);
			int nodeD = nodeIdProvider.apply(args[4]);
			return Optional.of(new Current2Current(args[0], nodeA, nodeB, nodeC, nodeD, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "VII";
	}
	
	@Override
	public String configInfo() {
		return String.format("%.03f A/A", this.factor);
	}
	
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	public double factor() {
		return factor;
	}
	
	@Override
	public int[] vsourceIds() {
		return new int[] { this.vid };
	}
	
	@Override
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {
			
		this.vid = ctx.nextVoltageSourceId();
		if (!ctx.stampOnlyZ()) {
			int mid = this.vid + ctx.nodeCount();
			if (this.nodeA != 0) {
				A.addM(mid, this.nodeA - 1, +1.0);
				A.addM(this.nodeA - 1, mid, +1.0);
			}
			if (this.nodeB != 0) {
				A.addM(mid, this.nodeB - 1, -1.0);
				A.addM(this.nodeB - 1, mid, -1.0);
			}
			if (this.nodeC != 0)
				A.addM(mid, this.nodeC - 1, -this.factor);
			if (this.nodeD != 0)
				A.addM(mid, this.nodeD - 1, +this.factor);
		}
		
	}
	
}
