package de.m_marvin.electronflow.nltisolver.elements;

import java.util.Optional;

import de.m_marvin.electronflow.nltisolver.DecimalPrefixFormater;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext;
import de.m_marvin.unimat.impl.MatrixNd;

public class VoltagePower extends TwoPort {
	
	protected double epsilon = 1;
	protected double voltage;
	protected double power;
	protected int vid;
	
	public VoltagePower(String name, String nodeA, String nodeB, double voltage, double power) {
		super(name, nodeA, nodeB);
		this.voltage = voltage;
		this.power = power;
	}
	
	public static Optional<Element> tryParse(String[] args) {
		if (args[0].startsWith("PV") && args.length == 5) {
			double value = DecimalPrefixFormater.parseDouble(args[3]);
			double value2 = DecimalPrefixFormater.parseDouble(args[4]);
			return Optional.of(new VoltagePower(args[0], args[1], args[2], value, value2));
		}
		return Optional.empty();
	}
	
	@Override
	public String type() {
		return "PV";
	}
	
	@Override
	public String configInfo() {
		return DecimalPrefixFormater.format("%s V %s W", this.voltage, this.power);
	}
	
	public void setVoltage(double voltage) {
		this.voltage = voltage;
	}
	
	public void setPower(double power) {
		this.power = power;
	}
	
	public double getVoltage() {
		return voltage;
	}
	
	public double getPower() {
		return power;
	}
	
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}
	
	public double getEpsilon() {
		return epsilon;
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
	public boolean isNonLinear() {
		return true;
	}
	
	protected double[] sourceLinear(double current) {
		double voltage;
		double factor; // V/A
		System.out.println("i trough gen: " + -current);
		if (-current > (this.power / this.voltage) + (this.epsilon / 2)) {
			System.out.println("zone 1");
			voltage = (2 * this.power) / -current;
			factor = this.power / Math.pow(-current, 2);
		} else if (-current > (this.power / this.voltage) - (this.epsilon / 2)) {
			
			double ilim = (this.power / this.voltage);
			
			double d = (-current - ilim) / (this.epsilon) + 0.5;
			System.out.println("zone 2 " + d);
			
//			lcmG = 1 / (this.model.resistanceOff + (this.model.resistanceOn - this.model.resistanceOff) * d);
//			lcmI = this.model.forwardVoltage * -lcmG * d;
			
			double flim = this.power / Math.pow(this.epsilon * 0.5 + ilim, 2);
			
			factor = flim * d;
			voltage = ;
			
//			voltage = (2 * this.power) / -current;
//			factor = - this.power / Math.pow(-current, 2);
		} else {
			System.out.println("zone 3");
			voltage = this.voltage;
			factor = 0;
		}
		return new double[] { voltage, factor };
	}
	
	@Override
	public void stampMatricies(StampingContext ctx, MatrixNd A, MatrixNd z) {
		
		double operatingCurrent = ctx.nlInit() ? 0.0 : this.network.getVSourceCurrent(this.vid);
		double[] lcm = sourceLinear(operatingCurrent);

		int midSource = this.vid + ctx.nodeCount();
		if (ctx.stampsZ()) {
			z.addM(0, midSource, lcm[0]);
		}
		if (ctx.stampsA()) {
//			int midMeter = this.vidMeter + ctx.nodeCount();
			if (this.nodeAid != 0) {
//				A.addM(midMeter, this.nodeAid - 1, +1.0);
//				A.addM(this.nodeAid - 1, midMeter, +1.0);
			}
			if (this.nodeBid != 0) {
//				A.addM(midMeter, this.nodeBid - 1, -1.0);
//				A.addM(this.nodeBid - 1, midMeter, -1.0);
			}
			if (this.nodeAid != 0) {
				A.addM(midSource, this.nodeAid - 1, +1.0);
				A.addM(this.nodeAid - 1, midSource, +1.0);
			}
			if (this.nodeBid != 0) {
				A.addM(midSource, this.nodeBid - 1, -1.0);
				A.addM(this.nodeBid - 1, midSource, -1.0);
			}
			A.addM(midSource, midSource, -lcm[1]);
		}
		
	}
	
}
