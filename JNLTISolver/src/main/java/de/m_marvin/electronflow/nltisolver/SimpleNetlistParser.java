package de.m_marvin.electronflow.nltisolver;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.m_marvin.electronflow.nltisolver.elements.Current;
import de.m_marvin.electronflow.nltisolver.elements.Current2Current;
import de.m_marvin.electronflow.nltisolver.elements.Current2Voltage;
import de.m_marvin.electronflow.nltisolver.elements.Diode;
import de.m_marvin.electronflow.nltisolver.elements.Element;
import de.m_marvin.electronflow.nltisolver.elements.Resistor;
import de.m_marvin.electronflow.nltisolver.elements.Voltage;
import de.m_marvin.electronflow.nltisolver.elements.Voltage2Current;
import de.m_marvin.electronflow.nltisolver.elements.Voltage2Voltage;
import de.m_marvin.electronflow.nltisolver.elements.VoltagePower;
import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork;

public class SimpleNetlistParser {
	
	@FunctionalInterface
	public static interface IComponentParser {
		public Optional<Element> tryParse(String[] args);
	}
	
	private final Set<IComponentParser> knownComponents;
	
	public SimpleNetlistParser(Set<IComponentParser> knownComponents) {
		this.knownComponents = knownComponents;
	}
	
	public SimpleNetlistParser() {
		this(Set.of(
				Voltage::tryParse,
				Current::tryParse,
				Resistor::tryParse,
				Voltage2Current::tryParse,
				Current2Current::tryParse,
				Voltage2Voltage::tryParse,
				Current2Voltage::tryParse,
				Diode::tryParse,
				VoltagePower::tryParse
		));
	}
	
	public Optional<Element> parseComponent(String[] args) {
		
		for (var comp : this.knownComponents) {
			Optional<Element> element = comp.tryParse(args);
			if (element.isPresent())
				return element;
		}
		return Optional.empty();
		
	}
	
	public Optional<IndexedNetwork> parseNet(Supplier<String> lineSupplier) {
		
		List<Element> elements = new ArrayList<>();
		
		String line;
		while ((line = lineSupplier.get()) != null) {
			
			if (line.isBlank()) continue;
			line = line.strip();
			if (line.startsWith("*") || line.startsWith("#") || line.startsWith("/")) continue;
			
			String[] args = line.replace('\t', ' ').split(" ");
			Optional<Element> element = parseComponent(args);
			
			if (!element.isPresent())
				return Optional.empty();
			
			elements.add(element.get());
			
		}
		
		return Optional.of(new IndexedNetwork(elements));
		
	}
	
	public Optional<IndexedNetwork> parseNet(BufferedReader reader) throws IOException {
		IOException[] ex = new IOException[1];
		Optional<IndexedNetwork> network = parseNet(() -> {
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
	
	public Optional<IndexedNetwork> parseNet(InputStream stream) throws IOException {
		return parseNet(new BufferedReader(new InputStreamReader(stream)));
	}
	
	public Optional<IndexedNetwork> parseNet(File netlistFile) throws IOException {
		return parseNet(new FileInputStream(netlistFile));
	}
	
	protected void printNetResult(IndexedNetwork network, Function<String, Boolean> lineConsumer) {
		for (var node : network.getNodes()) {
			double potential = network.getNodePotential(node);
			if (!lineConsumer.apply(String.format("%s\t%s V", node, DecimalPrefixFormater.formatDouble(potential))))
				return;
		}
		for (var comp : network.getElements()) {
			double[] currents = comp.currents();
			if (currents.length == 0) continue;
			StringBuffer lineBuffer = new StringBuffer();
			lineBuffer.append(comp.name());
			for (double current : currents)
				lineBuffer.append(String.format("\t%s A", DecimalPrefixFormater.formatDouble(current)));
			if (!lineConsumer.apply(lineBuffer.toString()))
				return;
		}
	}
	
	public void printNetResult(IndexedNetwork network, Consumer<String> lineConsumer) {
		printNetResult(network, line -> {
			lineConsumer.accept(line);
			return true;
		});
	}
	
	public void printNetResult(IndexedNetwork network, BufferedWriter writer) throws IOException {
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
	
	public void printNetResult(IndexedNetwork network, OutputStream stream) throws IOException {
		printNetResult(network, new BufferedWriter(new OutputStreamWriter(stream)));
	}
	
	public void printNetResult(IndexedNetwork network, File out) throws IOException {
		printNetResult(network, new FileOutputStream(out));
	}
	
}
