package test.esim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import test.esim.components.Capacitor;
import test.esim.components.Current;
import test.esim.components.Current2Current;
import test.esim.components.Resistor;
import test.esim.components.Voltage;
import test.esim.components.Voltage2Current;

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
				Capacitor::tryParse,
				Voltage2Current::tryParse,
				Current2Current::tryParse
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
			
			if (!component.isPresent()) {
				System.err.println("unknown component: " + line);
				return Optional.empty();
			}
			
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
	
	public void printNetResult(Network network, Consumer<String> lineConsumer) {
		lineConsumer.accept("- node potentials -");
		for (var node : this.nodeMap.entrySet()) {
			double potential = network.getNodePotential(node.getValue());
			lineConsumer.accept(String.format("%s %.03f V", node.getKey(), potential));
		}
		lineConsumer.accept("- vsource currents -");
		for (var comp : network.getComponents()) {
			if (comp.vsourceIds().length != 1) continue;
			double current = network.getVSourceCurrent(comp.name());
			lineConsumer.accept(String.format("%s %.03f A", comp.name(), current));
		}
		lineConsumer.accept("- end of data -");
	}
	
}
