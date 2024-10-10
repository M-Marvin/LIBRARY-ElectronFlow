package testing;

import de.m_marvin.nglink.NGLink;
import de.m_marvin.nglink.NGLink.INGCallback;
import de.m_marvin.nglink.NGLink.PlotDescription;
import de.m_marvin.nglink.NGLink.VectorValuesAll;

public class Test {
	
	public static void main(String[] args) {
		
		System.out.println("TEST");
		
		NGLink nglspice = new NGLink();
		
		nglspice.initNGLink(new INGCallback() {
			
			@Override
			public void reciveVecData(VectorValuesAll vecData, int vectorCount) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void reciveInitData(PlotDescription plotInfo) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void log(String s) {
				System.out.println(s);
			}
			
			@Override
			public void detacheNGSpice() {
				System.out.println("detach");
			}
		});
		
		if (!nglspice.initNGSpice()) {
			System.err.println("could not load spice");
		}
		
	}
	
}
