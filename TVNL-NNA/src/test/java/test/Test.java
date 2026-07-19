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
	
	public static void simulationTest(File netlist, NodalNetlistParser parser, double t0, double t1, double ts, Double tdump, boolean debugout) {
		
		try {

			NodalNetwork network = parser.parseNetlist(netlist);

			System.out.println("Network:\n" + network);
			
			System.out.println("System Matrices:");
			network.stampMatrices(StampingMode.FULL_MATRICES, 0, 0);
			System.out.println(network.getSystemMatrix_A());
			System.out.println(network.getSystemMatrix_E());
			System.out.println(network.getSystemMatrix_z());
			
			NodalNetworkSolver solver = NodalNetworkSolver_LAPACK.standard();
			if (debugout)
				solver.debug(s -> System.out.println("[SOLVER] " + s));

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
			
			double t;
			for (t = t0; t < t1; t+= ts) {
				
				try {

					System.out.println("-> STEP: " + t);
					
					solver.step(ts);
					
					System.out.println(network.getSystemMatrix_x());
					
					for (int i = 0; i < plotdata.length; i++) {
						plotdata[i].add(t, network.getSystemMatrix_x().m(0, i));
					}
					
				} catch (NetworkSolverException e) {
					e.printStackTrace();
					break;
				} finally {

					if (tdump != null && t == tdump) {
						System.out.println("-- SIMULATION STOP, DUMP INTERNAL STATE --");
						
						System.out.println("Differential Algebraic System: A*x+E*x'=z");
						System.out.println("-- A --");
						System.out.println(network.getSystemMatrix_A());
						System.out.println("-- E --");
						System.out.println(network.getSystemMatrix_E());
						System.out.println("-- z --");
						System.out.println(network.getSystemMatrix_z());
						System.out.println("-- x --");
						System.out.println(network.getSystemMatrix_x());
						
						System.out.println("-- Element Parameters --");
						for (var state : network.getElements()) {
							System.out.println(state.shortString());
							for (var e : state.parameters().entrySet())
								System.out.println(e.getKey() + " = " + e.getValue());
						}
						
						System.out.println("-- END OF INTENARL STATE DUMP --");
						System.exit(0);
						
					}
					
				}
				
			}
			
			if (t == t0) {
				System.out.println("-- SIMULATION FAILED --");
				System.exit(1);
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
		
//		File modelPath = new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "../run/stdmodels");
//		File netlistPath = new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "../run/testnets");
		
		File modelPath = new File(".");
		File netlistFile = null;
		double tstart = 0.0;
		double tstop = 10.0;
		double tstep = 0.1;
		Double tdump = null;
		boolean debugout = false;
		
		for (int i = 0; i < args.length; i++) {
			String s = args[i];
			if (s.startsWith("-")) {
				if (i < args.length -1) {
					if (s.endsWith("models")) {
						i++;
						modelPath = new File(args[i]);
					} else if (s.endsWith("netlist")) {
						i++;
						netlistFile = new File(args[i]);
					} else if (s.endsWith("tstart")) {
						i++;
						tstart = Double.parseDouble(args[i]);
					} else if (s.endsWith("tstop")) {
						i++;
						tstop = Double.parseDouble(args[i]);
					} else if (s.endsWith("tstep")) {
						i++;
						tstep = Double.parseDouble(args[i]);
					} else if (s.endsWith("tdump")) {
						i++;
						tdump = Double.parseDouble(args[i]);
					}
				} else {
					if (s.endsWith("debug")) {
						debugout = true;
					}
				}
			}
		}
		
		if (netlistFile == null || !netlistFile.isFile() || !modelPath.isDirectory()) {
			System.out.println("simtest -netlist *netlist* <-models *model dir* -tstart (default 0.0) -tstop (default 10.0) -tstep (default 0.1)>");
			System.exit(-1);
		}
		
		NodalNetlistParser parser = NodalNetlistParser.empty().loadElements(modelPath);
		
		simulationTest(netlistFile, parser, tstart, tstop, tstep, tdump, debugout);
		
//		simulationTest(new File(netlistPath, "v_load_test.efn"), parser, 0, 10, 1);
		
	}
	
}
