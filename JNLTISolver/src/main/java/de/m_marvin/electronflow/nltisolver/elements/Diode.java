package de.m_marvin.electronflow.nltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.nltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class Diode extends TwoPort {

	public static class DiodeModel {
		public double resistanceOn = 0.1;
		public double resistanceOff = 1E9;
		public double forwardVoltage = 0.0;
		public double epsilon = 0.0;
		public double breakdownVoltage = Double.NEGATIVE_INFINITY;
		public double breakdownResistance = 0.1;
		public double breakdownEpsilon = 0.0;
	}
	
	public static final DiodeModel DEFAULT_MODEL = new DiodeModel();
	
	protected DiodeModel model;

	public Diode(String name, String nodeA, String nodeB, DiodeModel model) {
		super(name, nodeA, nodeB);
		this.model = model;
	}

	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("D") && args.length >= 3) {
			DiodeModel model = DEFAULT_MODEL;
			for (int i = 3; i < args.length; i++) {
				String[] s = args[i].split("=");
				if (s.length != 2) continue;
				double d = DecimalPrefixFormater.parseDouble(s[1]);
				switch (s[0]) {
				case "Vfwd": model.forwardVoltage = d; break;
				case "Ron": model.resistanceOn = d; break;
				case "Roff": model.resistanceOff = d; break;
				case "Eps": model.epsilon = d; break;
				case "Vrev": model.breakdownVoltage = d; break;
				case "Rrev": model.breakdownResistance = d; break;
				case "EpsRev": model.breakdownEpsilon = d; break;
				}
			}
			return Optional.of(new Diode(args[0], args[1], args[2], model));
		}
		return Optional.empty();
	}
	
	public DiodeModel getModel() {
		return model;
	}
	
	public void setModel(DiodeModel model) {
		this.model = model;
	}
	
	@Override
	public String type() {
		return "D";
	}

	@Override
	public String configInfo() {
		return DecimalPrefixFormater.format("Vfwd=%s Ron=%s Roff=%s Eps=%s Vrev=%s Rrev=%s EpsRev=%s",
				this.model.forwardVoltage,
				this.model.resistanceOn,
				this.model.resistanceOff,
				this.model.epsilon,
				this.model.breakdownVoltage,
				this.model.breakdownResistance,
				this.model.breakdownEpsilon
		);
	}

	
	@Override
	public boolean isNonLinear() {
		return true;
	}
	
	@Override
	public double[] currents() {
		double p0 = this.network.getNodePotential(this.nodeAid);
		double p1 = this.network.getNodePotential(this.nodeBid);
		double v = p0 - p1;
		double[] lcm = diodeLinear(v);
		return new double[] {
				lcm[0] + v * lcm[1]
		};
	}
	
	protected double[] diodeLinear(double voltage) {
		double lcmG;
		double lcmI;
		if (voltage > this.model.forwardVoltage + (this.model.epsilon / 2)) {
			lcmG = 1 / this.model.resistanceOn;
			lcmI = this.model.forwardVoltage * -lcmG;
		} else if (voltage > this.model.forwardVoltage - (this.model.epsilon)) {
			double d = (voltage + this.model.forwardVoltage) / (2 * this.model.epsilon);
			lcmG = 1 / (this.model.resistanceOff + (this.model.resistanceOn - this.model.resistanceOff) * d);
			lcmI = this.model.forwardVoltage * -lcmG * d;
		} else if (voltage > this.model.breakdownVoltage + (this.model.breakdownEpsilon / 2)) {
			lcmG = 1 / this.model.resistanceOff;
			lcmI = 0;
		} else if (voltage > this.model.breakdownVoltage - (this.model.breakdownEpsilon / 2)) {
			double d = (voltage + this.model.breakdownVoltage) / (2 * this.model.breakdownEpsilon);
			lcmG = 1 / (this.model.breakdownResistance + (this.model.resistanceOff - this.model.breakdownResistance) * d);
			lcmI = this.model.breakdownVoltage * -lcmG * d;
		} else {
			lcmG = 1 / this.model.breakdownResistance;
			lcmI = this.model.breakdownVoltage * lcmG;
		}
		return new double[] { lcmI, lcmG };
	}
	
	@Override
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		double operatingVoltage = ctx.nlInit() ? this.model.forwardVoltage : this.network.getNodePotential(this.nodeA) - this.network.getNodePotential(this.nodeB);
		double[] lcm = diodeLinear(operatingVoltage);
		
		if (this.nodeAid != 0)
			A.addM(this.nodeAid - 1, this.nodeAid - 1, +lcm[1]);
		if (this.nodeBid != 0)
			A.addM(this.nodeBid - 1, this.nodeBid - 1, +lcm[1]);
		if (this.nodeAid != 0 && this.nodeBid != 0) {
			A.addM(this.nodeAid - 1, this.nodeBid - 1, -lcm[1]);
			A.addM(this.nodeBid - 1, this.nodeAid - 1, -lcm[1]);
		}
		if (this.nodeAid != 0)
			z.addM(0, this.nodeAid - 1, -lcm[0]);
		if (this.nodeBid != 0)
			z.addM(0, this.nodeBid - 1, +lcm[0]);
		
//		double lcmG;
//		double lcmI;
//		if (operatingVoltage > this.model.forwardVoltage + (this.model.epsilon / 2)) {
//			lcmG = 1 / this.model.resistanceOn;
//			lcmI = this.model.forwardVoltage * -lcmG;
//		} else if (operatingVoltage > this.model.forwardVoltage - (this.model.epsilon)) {
//			double d = (operatingVoltage + this.model.forwardVoltage) / (2 * this.model.epsilon);
//			lcmG = 1 / (this.model.resistanceOff + (this.model.resistanceOn - this.model.resistanceOff) * d);
//			lcmI = this.model.forwardVoltage * -lcmG * d;
//		} else if (operatingVoltage > this.model.breakdownVoltage + (this.model.breakdownEpsilon / 2)) {
//			lcmG = 1 / this.model.resistanceOff;
//			lcmI = 0;
//		} else if (operatingVoltage > this.model.breakdownVoltage - (this.model.breakdownEpsilon / 2)) {
//			double d = (operatingVoltage + this.model.breakdownVoltage) / (2 * this.model.breakdownEpsilon);
//			lcmG = 1 / (this.model.breakdownResistance + (this.model.resistanceOff - this.model.breakdownResistance) * d);
//			lcmI = this.model.breakdownVoltage * -lcmG * d;
//		} else {
//			lcmG = 1 / this.model.breakdownResistance;
//			lcmI = this.model.breakdownVoltage * lcmG;
//		}

//		if (this.nodeAid != 0)
//			A.addM(this.nodeAid - 1, this.nodeAid - 1, +lcmG);
//		if (this.nodeBid != 0)
//			A.addM(this.nodeBid - 1, this.nodeBid - 1, +lcmG);
//		if (this.nodeAid != 0 && this.nodeBid != 0) {
//			A.addM(this.nodeAid - 1, this.nodeBid - 1, -lcmG);
//			A.addM(this.nodeBid - 1, this.nodeAid - 1, -lcmG);
//		}
//		if (this.nodeAid != 0)
//			z.addM(0, this.nodeAid - 1, -lcmI);
//		if (this.nodeBid != 0)
//			z.addM(0, this.nodeBid - 1, +lcmI);
		
	}
	
}
