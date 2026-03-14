package de.m_marvin.electronflow.ltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.ltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Current extends TwoPort {
	
	protected double current;
	
	public Current(String name, String nodeA, String nodeB, double current) {
		super(name, nodeA, nodeB);
		this.current = current;
	}

	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("I") && args.length == 4) {
			double value = DecimalPrefixFormater.parseDouble(args[3]);
			return Optional.of(new Current(args[0], args[1], args[2], value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "I";
	}
	
	@Override
	public String configInfo() {
		return DecimalPrefixFormater.format("%s A", this.current);
	}
	
	public void setCurrent(double current) {
		this.current = current;
	}
	
	public double current() {
		return current;
	}
	
	@Override
	public double[] currents() {
		return new double[] {
				this.current
		};
	}
	
	@Override
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		if (ctx.stampsZ()) {
			if (this.nodeAid != 0)
				z.addM(0, this.nodeAid - 1, -this.current);
			if (this.nodeBid != 0)
				z.addM(0, this.nodeBid - 1, +this.current);
		}
		
	}

}
