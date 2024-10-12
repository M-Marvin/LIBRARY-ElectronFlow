package testing;

import de.m_marvin.electronflow.Solver;

public class Test {
	
	public static void main(String[] args) throws InterruptedException {
		
		Solver solver = new Solver();
		solver.attachElectronFlow();
		
		Thread.sleep(1000);
		
		String list = "gen_test\r\n"
				+ "\r\n"
				+ "* Generator 1\r\n"
				+ "R1 GN1 VG1 1m\r\n"
				+ "B1 GN1 0G1 V=min(10,1000/max(1m,+I(R1)))\r\n"
				+ "\r\n"
				+ "* Transformator 1\r\n"
				+ "ZT1 VT1 0T1 VG1 0G1 H[ 0.0 2.0 2.0 0.0 ]\r\n"
				+ "RT1 0G1 0T1 1G\r\n"
				+ "\r\n"
				+ "* Last\r\n"
				+ "R4 VT1 0T1 1\r\n"
				+ "";
		
		solver.upload(list);
		solver.execute("op");
		System.out.println(solver.printData());
		
		solver.detachElectronFlow();
		
	}
	
}
