package de.m_marvin.electronflow.nltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.nltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork;
import de.m_marvin.unimat.impl.MatrixNd;

public class DiodeNL extends TwoPort {
	
	protected double thermalVoltage;
	protected double saturationCurrent;
	protected double forwardVoltage = 0.7;
	
	public DiodeNL(String name, String nodeA, String nodeB, double thermalVoltage, double saturationCurrent) {
		super(name, nodeA, nodeB);
		this.thermalVoltage = thermalVoltage;
		this.saturationCurrent = saturationCurrent;
	}

	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("D") && args.length == 5) {
			double valueIs = DecimalPrefixFormater.parseDouble(args[3]);
			double valueUt = DecimalPrefixFormater.parseDouble(args[4]);
			return Optional.of(new DiodeNL(args[0], args[1], args[2], valueUt, valueIs));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "D";
	}

	@Override
	public String configInfo() {
		return DecimalPrefixFormater.format("%s Is %s Vt", this.saturationCurrent, this.thermalVoltage);
	}
	
	@Override
	public boolean isNonLinear() {
		return true;
	}
	
	@Override
	public double[] currents() {
		double p0 = this.network.getNodePotential(this.nodeAid);
		double p1 = this.network.getNodePotential(this.nodeBid);
		return new double[] {
				diodeCurrent(p0 - p1)
		};
	}
	
	protected double diodeCurrent(double voltage) {
		return this.saturationCurrent * (Math.exp(voltage / this.thermalVoltage) - 1);
	}
	
	@Override
	public void stampMatricies(IndexedNetwork.StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		double operatingVoltage = ctx.nlInit() ? this.forwardVoltage : this.network.getNodePotential(this.nodeA) - this.network.getNodePotential(this.nodeB);
		double operatingCurrent = diodeCurrent(operatingVoltage);
		double lcmG = (this.saturationCurrent / this.thermalVoltage) * Math.exp(operatingVoltage / this.thermalVoltage);
		double lcmI = operatingCurrent - lcmG * operatingVoltage;
		
		if (this.nodeAid != 0)
			A.addM(this.nodeAid - 1, this.nodeAid - 1, +lcmG);
		if (this.nodeBid != 0)
			A.addM(this.nodeBid - 1, this.nodeBid - 1, +lcmG);
		if (this.nodeAid != 0 && this.nodeBid != 0) {
			A.addM(this.nodeAid - 1, this.nodeBid - 1, -lcmG);
			A.addM(this.nodeBid - 1, this.nodeAid - 1, -lcmG);
		}
		if (this.nodeAid != 0)
			z.addM(0, this.nodeAid - 1, -lcmI);
		if (this.nodeBid != 0)
			z.addM(0, this.nodeBid - 1, +lcmI);
		
	}

}
