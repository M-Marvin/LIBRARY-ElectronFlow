package de.m_marvin.electronflow;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import de.m_marvin.electronflow.ltisolver.NetParser;
import de.m_marvin.electronflow.ltisolver.Network;
import de.m_marvin.electronflow.ltisolver.Solver;

public class Test {
	
	public static void main(String... args) throws IOException {
		
		File netlist = new File("./run/netlist.txt");
		File out = new File("./run/out.txt");
		
		NetParser parser = new NetParser();
		
		Optional<Network> parsed = parser.parseNet(netlist);
		
		if (parsed.isEmpty()) {
			System.err.println("unable to parse netlist");
			return;
		}
		
		Network network = parsed.get();
		
		System.out.println("= Test Network =");
		System.out.println(network);
		System.out.println("- - - - - - - - -");
		
		Solver solver = new Solver();
		
		network.stampMatrices(false);
		solver.initialize(network);
		solver.solve();
		
		parser.printNetResult(network, System.out::println);
		parser.printNetResult(network, out);
		
	}
	
}
