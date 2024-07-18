package de.m_marvin.electronflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.m_marvin.electronflow.NativeElectronFlow.Element;
import de.m_marvin.electronflow.NativeElectronFlow.Node;

public class ElectronFlowTest {
	
	public static void main(String... args) {
		
		File runDir = new File(ElectronFlowTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		System.out.println(runDir);
		
		ElectronFlow nef = new ElectronFlow();

		nef.setCallbacks((simtime, nodes, elements, nodecharge, timestep) -> {
			
			//System.out.println(simtime + "\t" + nodes.length + "\t" + nodecharge);
			
		}, (nodes, elemnents, nodecharge, timestep) -> {

			//System.out.println(nodes.length + "\t" + nodecharge);
			
			Map<String, Double> voltages = new HashMap<>();
			Map<String, Double> currents = new HashMap<>();
			
			for (Node node : nodes) {
				voltages.put(node.name, node.charge / nodecharge);
			}
			
			for (Element element : elemnents) {
				currents.put(element.name, element.transferCharge / timestep);
			}
			
			double v0 = voltages.getOrDefault("0", 0.0);
			
			double vi0 = voltages.getOrDefault("node|pos5_~60_8_id0_lid1_lnmn|", v0) - v0;
			double vi2 = voltages.getOrDefault("node|pos5_~60_8_id0_lid0_lnml|", v0) - v0;
			double ii = currents.getOrDefault("V1_55_pos5_~60_8", 0.0);
			
			double vo1 = voltages.getOrDefault("node|pos10_~60_8_id0_lid0_lnml|", v0) - v0;
			double vo2 = voltages.getOrDefault("node|pos10_~60_8_id0_lid1_lnmn|", v0) - v0;
			double io = currents.getOrDefault("R1_82", 0.0);
			
			
			
			double Vgen = vi2 - vi0;
			double Igen = ii;
			double Pgen = Vgen * Igen;
			
			double Vload = vo1 - vo2;
			double Iload = io;
			double Pload = Vload * Iload;
			
			System.out.println("Vgen " + Vgen + " -- Vload " + Vload);
			System.out.println("Igen " + Igen + " -- Iload " + Iload);
			System.out.println("Pgen " + Pgen + " -- Pload " + Pload);
			
		});
		
		try {
			File netlisteFile = new File(runDir, "../../../ElectronFlow/test/circuit_0.net");
			
			StringBuilder sb = new StringBuilder();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(netlisteFile)));
			String line;
			while ((line = reader.readLine()) != null) sb.append(line).append("\n");
			reader.close();
			String netlist = sb.toString();
			
			nef.loadAndRunNetList(netlist);
			
//			nef.controllCommand("step", "0.5u", "0.06m", "2m");
//
//			nef.controllCommand("printi");
			
			//nef.loadNetList(netlist);
			
			//nef.controllCommand("step 2m 600 30");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		nef.destroy();
		
	}
	
}
