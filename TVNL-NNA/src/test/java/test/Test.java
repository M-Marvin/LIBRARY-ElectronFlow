package test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import de.m_marvin.basicxml.XMLException;
import de.m_marvin.basicxml.marshaling.XMLMarshalingException;
import tvnlnna.NetworkSolverException;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalNetwork;
import tvnlnna.nodal.NodalNetwork.StampingContext.StampingMode;
import tvnlnna.parser.NodalNetlistParser;
import tvnlnna.solver.NodalNetworkSolver;
import tvnlnna.solver.NodalNetworkSolver_LAPACK;

public class Test {
	
	public static void simulationTest(File netlist, NodalNetlistParser parser, double t0, double t1, double ts) {
		
		try {

			NodalNetwork network = parser.parseNetlist(netlist);

			System.out.println("Network:\n" + network);
			
			System.out.println("System Matrices:");
			network.stampMatrices(StampingMode.FULL_MATRICES, 0, 0);
			System.out.println(network.getSystemMatrix_A());
			System.out.println(network.getSystemMatrix_E());
			System.out.println(network.getSystemMatrix_z());
			
			NodalNetworkSolver solver = NodalNetworkSolver_LAPACK.standard();

			System.out.println("Solver:\n" + solver);
			
			solver.setNetwork(network);
			solver.setSimulationTime(0.0);
			solver.resetAndInitSimulation();
			
			System.out.println("-- SIMULATION START --");
			
			XYSeries[] plotdata = new XYSeries[network.nodeCount() + network.localCount()];
			for (var element : network.getElements()) {
				for (int i = 0; i < element.type().locals().size(); i++) {
					plotdata[element.localIds()[i] + network.nodeCount()] = new XYSeries(element.name() + "/" + element.type().locals().get(i).name);
				}
			}
			for (int i = 1; i < network.getNodeNames().size(); i++) {
				plotdata[i - 1] = new XYSeries(network.getNodeNames().get(i));
			}
			
			for (double t = t0; t < t1; t+= ts) {
				
				try {
					
					solver.step(ts);
					
					System.out.println("STEP: " + t);
					System.out.println(network.getSystemMatrix_x());
					
					for (int i = 0; i < plotdata.length; i++) {
						plotdata[i].add(t, network.getSystemMatrix_x().m(0, i));
					}
					
				} catch (NetworkSolverException e) {
					e.printStackTrace();
					break;
				}
				
			}
			
			System.out.println("-- SIMULATION END --");
			
			XYSeriesCollection dataset = new XYSeriesCollection();
			for (var data : plotdata)
				dataset.addSeries(data);
			
			JFreeChart chart = ChartFactory.createXYLineChart("Simulation: " + netlist.getName(), "Time", "Value", dataset, PlotOrientation.VERTICAL, true, true, false);
			ChartPanel panel = new ChartPanel(chart);
			JFrame frame = new JFrame();
			frame.setContentPane(panel);
			frame.setSize(800, 600);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) throws URISyntaxException, XMLMarshalingException, IOException, XMLException, NodalMatrixStampException {
		
		File modelPath = new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "../run/stdmodels");
		File netlistPath = new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "../run/testnets");
		
		NodalNetlistParser parser = NodalNetlistParser.empty().loadElements(modelPath);
		
//		simulationTest(new File(netlistPath, "capacitor_charge.efn"), parser, 0, 20, 1);

//		simulationTest(new File(netlistPath, "source_change.efn"), parser, 0, 20, 1);

		simulationTest(new File(netlistPath, "seperate_nets.efn"), parser, 0, 10, 1);
		
	}
	
}
