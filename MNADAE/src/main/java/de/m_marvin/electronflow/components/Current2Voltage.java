package de.m_marvin.electronflow.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.electronflow.Component;
import de.m_marvin.electronflow.Network.Context;
import de.m_marvin.unimat.impl.MatrixNd;

public class Current2Voltage extends FourPort {

	protected double factor;
	protected int vidMeter;
	protected int vidSource;
	
	public Current2Voltage(String name, int nodeA, int nodeB, int nodeC, int nodeD, double factor) {
		super(name, nodeA, nodeB, nodeC, nodeD);
		this.factor = factor;
	}
	
	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("VIU") && args.length == 6) {
			double value = Double.parseDouble(args[5]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			int nodeC = nodeIdProvider.apply(args[3]);
			int nodeD = nodeIdProvider.apply(args[4]);
			return Optional.of(new Current2Voltage(args[0], nodeA, nodeB, nodeC, nodeD, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "VIU";
	}
	
	@Override
	public String configInfo() {
		return String.format("%.03f V/A", this.factor);
	}
	
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	public double factor() {
		return factor;
	}
	
	@Override
	public int[] vsourceIds() {
		return new int[] {
				this.vidSource,
				this.vidMeter
		};
	}
	
	@Override
	public void stampMatricies(Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {

		this.vidSource = ctx.nextVoltageSourceId();
		this.vidMeter = ctx.nextVoltageSourceId();
		int midSource = this.vidSource + ctx.nodeCount();
		int midMeter = this.vidMeter + ctx.nodeCount();
		if (this.nodeA != 0) {
			A.addM(midMeter, this.nodeA - 1, +1.0);
			A.addM(this.nodeA - 1, midMeter, +1.0);
		}
		if (this.nodeB != 0) {
			A.addM(midMeter, this.nodeB - 1, -1.0);
			A.addM(this.nodeB - 1, midMeter, -1.0);
		}
		if (this.nodeC != 0) {
			A.addM(midSource, this.nodeC - 1, +1.0);
			A.addM(this.nodeC - 1, midSource, +1.0);
		}
		if (this.nodeD != 0) {
			A.addM(midSource, this.nodeD - 1, -1.0);
			A.addM(this.nodeD - 1, midSource, -1.0);
		}
		A.addM(midMeter, midSource, -this.factor);
		
	}
	
}
