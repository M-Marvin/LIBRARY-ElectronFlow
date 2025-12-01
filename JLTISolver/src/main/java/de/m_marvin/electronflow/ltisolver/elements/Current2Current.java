package de.m_marvin.electronflow.ltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Current2Current extends FourPort {
	
	protected double factor;
	protected int vid;
	
	public Current2Current(String name, String nodeA, String nodeB, String nodeC, String nodeD, double factor) {
		super(name, nodeA, nodeB, nodeC, nodeD);
		this.factor = factor;
	}
	
	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("VII") && args.length == 6) {
			double value = Double.parseDouble(args[5]);
			return Optional.of(new Current2Current(args[0], args[1], args[2], args[3], args[4], value));
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
		return new int[] {
				this.vid
		};
	}
	
	@Override
	public double[] currents() {
		double i = this.network.getVSourceCurrent(this.vid);
		return new double[] {
				i,
				i * this.factor
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
			if (this.nodeAid != 0) {
				A.addM(mid, this.nodeAid - 1, +1.0);
				A.addM(this.nodeAid - 1, mid, +1.0);
			}
			if (this.nodeBid != 0) {
				A.addM(mid, this.nodeBid - 1, -1.0);
				A.addM(this.nodeBid - 1, mid, -1.0);
			}
			if (this.nodeCid != 0)
				A.addM(mid, this.nodeCid - 1, -this.factor);
			if (this.nodeDid != 0)
				A.addM(mid, this.nodeDid - 1, +this.factor);
		}
		
	}
	
}
