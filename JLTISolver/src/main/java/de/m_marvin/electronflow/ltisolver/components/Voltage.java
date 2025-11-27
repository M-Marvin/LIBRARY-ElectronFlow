package de.m_marvin.electronflow.ltisolver.components;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.electronflow.ltisolver.Component;
import de.m_marvin.electronflow.ltisolver.Network;
import de.m_marvin.unimat.impl.MatrixNd;

public class Voltage extends TwoPort {
	
	protected double voltage;
	protected int vid;
	
	public Voltage(String name, int nodeA, int nodeB, double voltage) {
		super(name, nodeA, nodeB);
		this.voltage = voltage;
	}

	public static Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("U") && args.length == 4) {
			double value = Double.parseDouble(args[3]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			return Optional.of(new Voltage(args[0], nodeA, nodeB, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "U";
	}

	@Override
	public String configInfo() {
		return String.format("%.03f V", this.voltage);
	}
	
	public void setVoltage(double voltage) {
		this.voltage = voltage;
	}
	
	public double voltage() {
		return voltage;
	}

	@Override
	public int[] vsourceIds() {
		return new int[] { this.vid };
	}

	@Override
	public void stampMatricies(Network.StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		this.vid = ctx.nextVoltageSourceId();
		int mid = vid + ctx.nodeCount();
		z.addM(0, mid, this.voltage);
		if (!ctx.stampOnlyZ()) {
			if (this.nodeA != 0) {
				A.addM(mid, this.nodeA - 1, +1.0);
				A.addM(this.nodeA - 1, mid, +1.0);
			}
			if (this.nodeB != 0) {
				A.addM(mid, this.nodeB - 1, -1.0);
				A.addM(this.nodeB - 1, mid, -1.0);
			}
		}
		
	}

}
