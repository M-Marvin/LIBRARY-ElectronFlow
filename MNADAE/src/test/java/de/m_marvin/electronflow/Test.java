package de.m_marvin.electronflow;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Test {
	
	public static void main(String... args) throws IOException {
		
		File netlist = new File("./run/netlist.txt");
		
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
		
		network.fillMatrices();
		
		network.solveLinear();
		
		parser.printNetResult(network, System.out::println);
		
	}
	
}
