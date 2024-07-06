package de.m_marvin.electronflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ElectronFlowTest {
	
	public static void main(String... args) {
		
		File runDir = new File(ElectronFlowTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		System.out.println(runDir);
		
		ElectronFlow nef = new ElectronFlow();

		nef.setCallbacks((simtime, nodes, nodecharge) -> {
			
			System.out.println(simtime + "\t" + nodes.length + "\t" + nodecharge);
			
		}, (nodes, nodecharge) -> {

			System.out.println(nodes.length + "\t" + nodecharge);
			
		});
		
		try {
			File netlisteFile = new File(runDir, "../../../ElectronFlow/test/trafo.net");
			
			StringBuilder sb = new StringBuilder();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(netlisteFile)));
			String line;
			while ((line = reader.readLine()) != null) sb.append(line).append("\n");
			reader.close();
			String netlist = sb.toString();
			
			nef.loadNetList(netlist);
			
			nef.controllCommand("step 2m 600 30");
			
			nef.loadNetList(netlist);
			
			nef.controllCommand("step 2m 600 30");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		nef.destroy();
		
	}
	
}
