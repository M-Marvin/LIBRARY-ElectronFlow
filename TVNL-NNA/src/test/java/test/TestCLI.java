package test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import de.m_marvin.basicxml.XMLException;
import de.m_marvin.basicxml.marshaling.XMLMarshalingException;
import de.m_marvin.cliutil.arguments.Arguments;
import de.m_marvin.cliutil.arguments.CommandArgumentParser;
import tvnlnna.NetworkSolverException;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalNetwork;
import tvnlnna.nodal.NodalNetwork.StampingContext.StampingMode;
import tvnlnna.parser.NodalNetlistParser;
import tvnlnna.solver.NodalNetworkSolver;
import tvnlnna.solver.NodalNetworkSolver_LAPACK;

/**
 * A command line tool implementation for testing the simulation system.
 */
public class TestCLI {
	
	public static void simulationTest(File netlist, NodalNetworkSolver solver, NodalNetlistParser parser, double t0, double t1, double ts, LogMode output, boolean showGraph) {
		
		try {
			
			NodalNetwork network = parser.parseNetlist(netlist);

			if (output.id() >= 1) {
				System.out.println("[--- Simulation Netlist ---]");
				System.out.println(network);
			}
			
			network.stampMatrices(StampingMode.FULL_MATRICES, 0, 0);
			
			if (output.id >= 2) {
				System.out.println("[--- System Matrizes (initial) ---]");
				System.out.println("A:\n" + network.getSystemMatrix_A());
				System.out.println("E:\n" + network.getSystemMatrix_E());
				System.out.println("z:\n" + network.getSystemMatrix_z());
			}
			
			solver.setNetwork(network);
			solver.resetAndInitSimulation();
			solver.setSimulationTime(t0);
			
			if (output.id() >= 3)
				solver.debug(s -> System.out.println("[SOLVER] " + s));
			
			if (output.id() >= 1) {
				System.out.println("[--- Solver Configuration ---]");
				System.out.println(solver);
			}
			
			if (output.id() >= 2) {
				System.out.println("[--- Simulation Start ---]");
			}

			long tstart = System.nanoTime();
			XYSeries[] plotdata = new XYSeries[network.nodeCount() + network.localCount()];
			if (showGraph) {
				for (var element : network.getElements()) {
					for (int i = 0; i < element.type().locals().size(); i++) {
						plotdata[element.localIds()[i] + network.nodeCount()] = new XYSeries(element.name() + "/" + element.type().locals().get(i).name);
					}
				}
				int o = network.getZeroNode() == null ? 0 : 1;
				for (int i = o; i < network.getNodeNames().size(); i++) {
					plotdata[i - o] = new XYSeries(network.getNodeNames().get(i));
				}
			}
			
			int n = (int) Math.ceil((t1 - t0) / ts) + 1;
			int i;
			for (i = 0; i < n; i++) {
				double t = t0 + ts * i;
				
				try {
					
					solver.step(ts);

					if (output.id() >= 2) {
						System.out.println("-> T: " + t);
						System.out.println(network.getSystemMatrix_x());
					}
					
					if (showGraph) {
						for (int i1 = 0; i1 < plotdata.length; i1++) {
							plotdata[i1].add(t, network.getSystemMatrix_x().m(0, i1));
						}
					}
					
				} catch (NetworkSolverException e) {
					System.err.println("error during simulation stepping:");
					printErrorStack(e);
					System.exit(1);
				}
				
			}
			long tend = System.nanoTime();
			
			if (i != n) {
				if (output.id() >= 1)
					System.out.println("[--- Simulation Failed ---]");
				System.exit(1);
			}

			if (output.id() >= 1) {
				System.out.println("[--- Simulation Completed ---]");
				System.out.println("completed in " + ((tend - tstart) / 1000000.0) + " ms");
			}
			
			if (output.id() >= 3) {
				System.out.println("[--- Internal State Dump ---]");
				
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
				
				System.out.println("[--- End of Internal State Dump ---]");
			}
			
			if (showGraph) {
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
			}
			
		} catch (Exception e) {
			System.err.println("error during simulation initialization:");
			printErrorStack(e);
			System.exit(1);
		}
		
	}
	
	public static void printErrorStack(Throwable e) {
		do {
			System.err.println(e.getMessage());
			e = e.getCause();
		} while (e != null);
	}
	
	public static enum LogMode {
		SILENT(0),
		MINIMAL(1),
		VERBOSE(2),
		DEBUG(3);
		
		private final int id;
		
		private LogMode(int id) {
			this.id = id;
		}
		
		public int id() {
			return this.id;
		}
	}
	
	public static void main(String[] args) throws URISyntaxException, XMLMarshalingException, IOException, XMLException, NodalMatrixStampException {
		
		CommandArgumentParser cmdparser = new CommandArgumentParser();
		cmdparser.addOption("help", false, "display this help info");
		cmdparser.addOption("models", new File("."), "the folder containing the model definition xml files");
		cmdparser.addOption("netlist", new File("."), "the netlist file to load and simulate");
		cmdparser.addOption("tsim", "0.0:10.0:0.01", "the simulation time span and step size, <start:stop:step>");
		cmdparser.addOption("tramp", "0.0:0.0", "the ramp up time span in which sources are remped up from zero, <start:end>");
		cmdparser.addOption("absutol", NodalNetworkSolver_LAPACK.DEFAULT_LIMUDABS, "the absolute potential tolerance of the nodes");
		cmdparser.addOption("relutol", NodalNetworkSolver_LAPACK.DEFAULT_LIMUDREL, "the relative potential tolerance of the nodes");
		cmdparser.addOption("absitol", NodalNetworkSolver_LAPACK.DEFAULT_LIMIDABS, "the absolute additonal variable tolerance of the elements");
		cmdparser.addOption("relitol", NodalNetworkSolver_LAPACK.DEFAULT_LIMIDREL, "the relative additonal variable tolerance of the elements");
		cmdparser.addOption("iterlim", NodalNetworkSolver_LAPACK.DEFAULT_LIMITER, "the non linear solver iteration limit");
		cmdparser.addOption("output", LogMode.SILENT, LogMode::valueOf, LogMode::name, "log output level <SILENT|MINIMAL|VERBOSE|DEBUG>");
		cmdparser.addOption("nograph", false, "if set, prevents the graphical graph to open");
		
		try {

			Arguments cmdargs = cmdparser.parse(args);
			
			// print help
			if (args.length == 0 || cmdargs.flag("help")) {
				System.out.println(cmdparser.printHelp());
				System.exit(1);
			}
			
			// parse input files
			File modelPath = cmdargs.get("models");
			if (!modelPath.isDirectory()) {
				System.err.println("folder not found: " + modelPath.getAbsoluteFile());
				System.exit(-1);
			}
			File netlistFile = cmdargs.get("netlist");
			if (!netlistFile.isFile()) {
				System.err.println("file not found: " + netlistFile.getAbsoluteFile());
				System.exit(-1);
			}
			
			// parse time span parameters
			Matcher tstr = Pattern.compile("([\\w+-.]+):([\\w+-.]+):([\\w+-.]+)").matcher(cmdargs.get("tsim"));
			if (!tstr.find()) {
				System.out.println("invalid step time argument: " + cmdargs.get("tsim"));
				System.exit(1);
			}
			double tstart = Double.parseDouble(tstr.group(1));
			double tstop = Double.parseDouble(tstr.group(2));
			double tstep = Double.parseDouble(tstr.group(3));
			tstr = Pattern.compile("([\\w+-.]+):([\\w+-.]+)").matcher(cmdargs.get("tramp"));
			if (!tstr.find()) {
				System.out.println("invalid ramp time argument: " + cmdargs.get("tramp"));
				System.exit(1);
			}
			double rstart = Double.parseDouble(tstr.group(1));
			double rend = Double.parseDouble(tstr.group(2));
			
			// parse output mode
			LogMode logMode = cmdargs.get("output");
			boolean noGraph = cmdargs.get("nograph");
			
			// parse solver parameters
			NodalNetworkSolver solver = NodalNetworkSolver_LAPACK.standard()
					.relItol(cmdargs.get("relitol"))
					.absItol(cmdargs.get("absitol"))
					.relUtol(cmdargs.get("relutol"))
					.absUtol(cmdargs.get("absutol"))
					.iterLim(cmdargs.get("iterlim"))
					.setRampup(rstart, rend);
			
			try {

				NodalNetlistParser parser = NodalNetlistParser.empty().loadElements(modelPath);
				
				simulationTest(netlistFile, solver, parser, tstart, tstop, tstep, logMode, !noGraph);
				
			} catch (Exception e) {
				System.err.println("error during model loading:");
				printErrorStack(e);
				System.exit(1);
			}
			
		} catch (Exception e) {
			System.err.println("error during argument parsing");
			printErrorStack(e);
			System.exit(-1);
		}
		
	}
	
}
