package de.m_marvin.electronflow.ltisolver.elements;

import java.util.Optional;
import java.util.function.Function;

import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Current extends TwoPort {
	
	protected double current;
	
	public Current(String name, int nodeA, int nodeB, double current) {
		super(name, nodeA, nodeB);
		this.current = current;
	}

	public static Optional<Element> tryParse(String[] args, Function<String, Integer> nodeIdProvider) {
		if (args[0].startsWith("I") && args.length == 4) {
			double value = Double.parseDouble(args[3]);
			int nodeA = nodeIdProvider.apply(args[1]);
			int nodeB = nodeIdProvider.apply(args[2]);
			return Optional.of(new Current(args[0], nodeA, nodeB, value));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "I";
	}
	
	@Override
	public String configInfo() {
		return String.format("%.03f A", this.current);
	}
	
	public void setCurrent(double current) {
		this.current = current;
	}
	
	public double current() {
		return current;
	}
	
	@Override
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {

		if (ctx.stampsZ()) {
			if (this.nodeA != 0)
				z.addM(0, this.nodeA - 1, -this.current);
			if (this.nodeB != 0)
				z.addM(0, this.nodeB - 1, +this.current);
		}
		
	}

}
