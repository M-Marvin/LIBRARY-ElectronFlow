package test;

import alglib.alglib;
import alglib.alglib.exception;
import alglib.alglib.minnsresults_results;
import alglib.alglib.minnsstate;
import alglib.alglib.ndimensional_fvec;
import alglib.alglib.ndimensional_rep;

public class Test {

	public static void main(String[] args) throws exception {
		
		
		double diffStep = 0.0;
		double[] startingPointsX0 = new double[] {};
		double radius = 0.0;
		double penalty = 0.0;
		double eps = 1.0;
		int itrLimit = 0;
		
		ndimensional_fvec fvec = new ndimensional_fvec() {

			@Override
			public void calc(double[] arg, double[] fi, Object obj) throws exception {
				// TODO Auto-generated method stub
				
			}
		};
		ndimensional_rep rep = new ndimensional_rep() {

			@Override
			public void report(double[] arg, double func, Object obj) throws exception {
				// TODO Auto-generated method stub
				
			}
		};
		
		minnsstate minns = alglib.minnscreatef(startingPointsX0, diffStep);
		alglib.minnssetalgoags(minns, radius, penalty);
		
		//alglib.minnssetbc(minns, variableBoundsLower, variableBoundsUpper);
		//alglib.minnssetlc(minns, null, null);
		//alglib.minnssetnlc(minns, 0, 0);
		//alglib.minnssetscale(minns, null);
		
		alglib.minnssetcond(minns, eps, itrLimit);
		
		alglib.minnsoptimize(minns, fvec, rep, null);
		
		minnsresults_results result = alglib.minnsresults(minns);
		
		
		
	}
	
}
