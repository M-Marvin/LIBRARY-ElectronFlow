package test.esim.elements;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.unimat.impl.MatrixNd;
import test.esim.Component;
import test.esim.Network;

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
	public void stampMatricies(Network.Context ctx, MatrixNd A, MatrixNd E, MatrixNd z) {
		
		this.vid = ctx.nextVoltageSourceId();
		int mid = vid + ctx.nodeCount();
		z.set(0, mid, this.voltage);
		A.set(mid, this.nodeA, +1.0);
		A.set(this.nodeA, mid, +1.0);
		A.set(mid, this.nodeB, -1.0);
		A.set(this.nodeB, mid, -1.0);
		
	}

}
