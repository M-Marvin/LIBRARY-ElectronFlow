package de.m_marvin.electronflow.ltisolver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.m_marvin.electronflow.ltisolver.components.Current;
import de.m_marvin.electronflow.ltisolver.components.Current2Current;
import de.m_marvin.electronflow.ltisolver.components.Current2Voltage;
import de.m_marvin.electronflow.ltisolver.components.Resistor;
import de.m_marvin.electronflow.ltisolver.components.Voltage;
import de.m_marvin.electronflow.ltisolver.components.Voltage2Current;
import de.m_marvin.electronflow.ltisolver.components.Voltage2Voltage;

public class NetParser {
	
	@FunctionalInterface
	public static interface IComponentParser {
		public Optional<Component> tryParse(String[] args, Function<String, Integer> nodeIdProvider);
	}
	
	private final Set<IComponentParser> knownComponents;
	private Map<String, Integer> nodeMap = new HashMap<>();
	
	public NetParser(Set<IComponentParser> knownComponents) {
		this.knownComponents = knownComponents;
	}
	
	public NetParser() {
		this(Set.of(
				Voltage::tryParse,
				Current::tryParse,
				Resistor::tryParse,
				Voltage2Current::tryParse,
				Current2Current::tryParse,
				Voltage2Voltage::tryParse,
				Current2Voltage::tryParse
		));
	}
	
	public int getNodeId(String nodeName) {
		return this.nodeMap.get(nodeName);
	}
	
	public Optional<Component> parseComponent(String[] args) {
		
		for (var comp : this.knownComponents) {
			Optional<Component> component = comp.tryParse(args, nodeName -> {
				Integer id = this.nodeMap.getOrDefault(nodeName, this.nodeMap.size());
				if (!this.nodeMap.containsKey(nodeName))
					this.nodeMap.put(nodeName, id);
				return id;
			});
			if (component.isPresent())
				return component;
		}
		return Optional.empty();
		
	}
	
	public Optional<Network> parseNet(Supplier<String> lineSupplier) {
		
		List<Component> components = new ArrayList<>();
		
		String line;
		while ((line = lineSupplier.get()) != null) {
			
			if (line.isBlank()) continue;
			line = line.strip();
			if (line.startsWith("*") || line.startsWith("#") || line.startsWith("/")) continue;
			
			String[] args = line.split(" ");
			Optional<Component> component = parseComponent(args);
			
			if (!component.isPresent())
				return Optional.empty();
			
			components.add(component.get());
			
		}
		
		return Optional.of(new Network(components));
		
	}
	
	public Optional<Network> parseNet(BufferedReader reader) throws IOException {
		IOException[] ex = new IOException[1];
		Optional<Network> network = parseNet(() -> {
			try {
				return reader.readLine();
			} catch (IOException e) {
				ex[0] = e;
				return null;
			}
		});
		reader.close();
		if (ex[0] != null) {
			throw ex[0];
		}
		return network;
	}
	
	public Optional<Network> parseNet(InputStream stream) throws IOException {
		return parseNet(new BufferedReader(new InputStreamReader(stream)));
	}
	
	public Optional<Network> parseNet(File netlistFile) throws IOException {
		return parseNet(new FileInputStream(netlistFile));
	}
	
	protected void printNetResult(Network network, Function<String, Boolean> lineConsumer) {
		for (var node : this.nodeMap.entrySet()) {
			double potential = network.getNodePotential(node.getValue());
			if (!lineConsumer.apply(String.format("%s\t%.03f V", node.getKey(), potential)))
				return;
		}
		for (var comp : network.getComponents()) {
			if (comp.vsourceIds().length == 0) continue;
			double[] currents = network.getVSourceCurrent(comp.name());
			StringBuffer lineBuffer = new StringBuffer();
			lineBuffer.append(comp.name());
			for (double current : currents)
				lineBuffer.append(String.format("\t%.03f A", current));
			if (!lineConsumer.apply(lineBuffer.toString()))
				return;
		}
	}
	
	public void printNetResult(Network network, Consumer<String> lineConsumer) {
		printNetResult(network, line -> {
			lineConsumer.accept(line);
			return true;
		});
	}
	
	public void printNetResult(Network network, BufferedWriter writer) throws IOException {
		IOException[] ex = new IOException[1];
		printNetResult(network, line -> {
			try {
				writer.append(line);
				writer.newLine();
				return true;
			} catch (IOException e) {
				ex[0] = e;
				return false;
			}
		});
		writer.close();
		if (ex[0] != null) {
			throw ex[0];
		}
	}
	
	public void printNetResult(Network network, OutputStream stream) throws IOException {
		printNetResult(network, new BufferedWriter(new OutputStreamWriter(stream)));
	}
	
	public void printNetResult(Network network, File out) throws IOException {
		printNetResult(network, new FileOutputStream(out));
	}
	
}
