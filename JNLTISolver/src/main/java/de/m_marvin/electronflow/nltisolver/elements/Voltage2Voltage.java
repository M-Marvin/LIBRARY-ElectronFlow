package de.m_marvin.electronflow.nltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.nltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Voltage2Voltage extends FourPort {
	
	protected double factor;
	protected int vid;
	
	public Voltage2Voltage(String name, String nodeA, String nodeB, String nodeC, String nodeD, double factor) {
		super(name, nodeA, nodeB, nodeC, nodeD);
		this.factor = factor;
	}
	
	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("VUU") && args.length == 6) {
			double value = DecimalPrefixFormater.parseDouble(args[5]);
			return Optional.of(new Voltage2Voltage(args[0], args[1], args[2], args[3], args[4], value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "VUU";
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
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {

		if (ctx.stampsA()) {
			int mid = this.vid + ctx.nodeCount();
			if (this.nodeAid != 0)
				A.addM(this.nodeAid - 1, mid, -this.factor);
			if (this.nodeBid != 0)
				A.addM(this.nodeBid - 1, mid, +this.factor);
			if (this.nodeCid != 0) {
				A.addM(mid, this.nodeCid - 1, +1.0);
				A.addM(this.nodeCid - 1, mid, +1.0);
			}
			if (this.nodeDid != 0) {
				A.addM(mid, this.nodeDid - 1, -1.0);
				A.addM(this.nodeDid - 1, mid, -1.0);
			}
		}
		
	}
	
}
