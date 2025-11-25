package test.esim.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.unimat.impl.MatrixNd;
import test.esim.Component;
import test.esim.Network.Context;

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
	public void stampMatricies(Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {
			
		this.vid = ctx.nextVoltageSourceId();
		int mid = this.vid + ctx.nodeCount();
		z.set(0, mid, 0.0);
		A.set(mid, this.nodeA, +1.0);
		A.set(this.nodeA, mid, +1.0);
		A.set(mid, this.nodeB, -1.0);
		A.set(this.nodeB, mid, -1.0);
		A.set(mid, this.nodeC, -this.factor);
		A.set(mid, this.nodeD, +this.factor);
		
	}
	
}
