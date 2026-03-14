package de.m_marvin.electronflow.nltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.nltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Voltage extends TwoPort {
	
	protected double voltage;
	protected int vid;
	
	public Voltage(String name, String nodeA, String nodeB, double voltage) {
		super(name, nodeA, nodeB);
		this.voltage = voltage;
	}

	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("U") && args.length == 4) {
			double value = DecimalPrefixFormater.parseDouble(args[3]);
			return Optional.of(new Voltage(args[0], args[1], args[2], value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "U";
	}

	@Override
	public String configInfo() {
		return DecimalPrefixFormater.format("%s", this.voltage);
	}
	
	public void setVoltage(double voltage) {
		this.voltage = voltage;
	}
	
	public double voltage() {
		return voltage;
	}

	@Override
	public int[] vsourceIds() {
		return new int[] {
				this.vid
		};
	}
	
	@Override
	public double[] currents() {
		return new double[] {
				this.network.getVSourceCurrent(this.vid)
		};
	}

	@Override
	public void index(StampingContext ctx) {
		super.index(ctx);
		this.vid = ctx.nextVoltageSourceId();
	}
	
	@Override
	public void stampMatricies(IndexedNetwork.StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		int mid = vid + ctx.nodeCount();
		if (ctx.stampsZ()) {
			z.addM(0, mid, this.voltage);
		}
		if (ctx.stampsA()) {
			if (this.nodeAid != 0) {
				A.addM(mid, this.nodeAid - 1, +1.0);
				A.addM(this.nodeAid - 1, mid, +1.0);
			}
			if (this.nodeBid != 0) {
				A.addM(mid, this.nodeBid - 1, -1.0);
				A.addM(this.nodeBid - 1, mid, -1.0);
			}
		}
		
	}

}
