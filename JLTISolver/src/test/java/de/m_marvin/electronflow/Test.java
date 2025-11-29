package de.m_marvin.electronflow;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import de.m_marvin.electronflow.ltisolver.NetlistParser;
import de.m_marvin.electronflow.ltisolver.elements.Voltage;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork;
import de.m_marvin.electronflow.ltisolver.network.NetworkSolver;
import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext.StampingMode;

public class Test {
	
	public static void main(String... args) throws IOException {
		
		File netlist = new File("./run/netlist.txt");
		File out = new File("./run/out.txt");
		
		NetlistParser parser = new NetlistParser();
		
		Optional<IndexedNetwork> parsed = parser.parseNet(netlist);
		
		if (parsed.isEmpty()) {
			System.err.println("unable to parse netlist");
			return;
		}
		
		IndexedNetwork network = parsed.get();
		
		System.out.println("= Test Network =");
		System.out.println(network);
		System.out.println("- - - - - - - - -");
		
		NetworkSolver solver = new NetworkSolver();
		
		network.stampMatrices(StampingMode.FULL_MATRICES);
		solver.initialize(network);
		solver.solve();
		
		parser.printNetResult(network, System.out::println);
		
		for (var c : network.getComponents()) {
			if (c instanceof Voltage v) {
				v.setVoltage(120);
				System.out.println("changed voltage");
				break;
			}
		}
		
		network.stampMatrices(StampingMode.FORCING_VECTOR);
		solver.solve();

		parser.printNetResult(network, System.out::println);
		
		parser.printNetResult(network, out);
		
	}
	
}
