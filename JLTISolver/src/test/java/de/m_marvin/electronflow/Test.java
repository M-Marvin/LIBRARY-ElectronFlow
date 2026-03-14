package de.m_marvin.electronflow;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import de.m_marvin.electronflow.ltisolver.SimpleNetlistParser;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext.StampingMode;
import de.m_marvin.electronflow.ltisolver.solver.NetworkSolver;
import de.m_marvin.electronflow.ltisolver.solver.NetworkSolverException;

public class Test {
	
	public static void main(String... args) throws IOException, NetworkSolverException {
		
		File netlist = new File("./run/netlist.txt");
		File out = new File("./run/out.txt");
		
		SimpleNetlistParser parser = new SimpleNetlistParser();
		
		Optional<IndexedNetwork> parsed = parser.parseNet(netlist);
		
		if (parsed.isEmpty()) {
			System.err.println("unable to parse netlist");
			return;
		}
		
		IndexedNetwork network = parsed.get();
		
		System.out.println("= Test Network =");
		System.out.println(network);
		System.out.println("- - - - - - - - -");
		
		try {
			NetworkSolver.standard().debug(System.out::println).limSingular(1E-20).iterLim(500).solve(network);
		} catch (Exception e) {e
			.printStackTrace();
		}
			
		parser.printNetResult(network, System.out::println);
		
	}
	
}
