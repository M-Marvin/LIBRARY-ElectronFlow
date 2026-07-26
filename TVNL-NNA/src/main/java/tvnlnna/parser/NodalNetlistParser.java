package tvnlnna.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import de.m_marvin.basicxml.XMLException;
import de.m_marvin.basicxml.XMLInputStream;
import de.m_marvin.basicxml.marshaling.XMLMarshalingException;
import de.m_marvin.basicxml.marshaling.XMLUnmarshaler;
import tvnlnna.nodal.NodalElement;
import tvnlnna.nodal.NodalElementState;
import tvnlnna.nodal.NodalNetwork;

public class NodalNetlistParser {
	
	public static final XMLUnmarshaler ELEMENT_XML_PARSER = new XMLUnmarshaler(true, NodalElement.class);
	
	private final Map<String, NodalElement> elements = new HashMap<>();
	
	private NodalNetlistParser() {}
	
	public static NodalNetlistParser empty() {
		return new NodalNetlistParser();
	}
	
	public NodalNetlistParser addElements(Collection<NodalElement> elements) {
		for (var e : elements)
			this.elements.put(e.name(), e);
		return this;
	}
	
	public NodalNetlistParser loadElements(File folder) throws XMLException, IOException {
		return loadElements(Stream.of(folder.listFiles()).filter(f -> f.isFile()).toList());
	}

	public NodalNetlistParser loadElements(Collection<File> files) throws XMLException, IOException {
		for (File f : files) {
			try {
				FileInputStream fin = new FileInputStream(f);
				XMLInputStream xin = new XMLInputStream(fin);
				NodalElement element = ELEMENT_XML_PARSER.unmarshall(xin, NodalElement.class);
				if (element == null)
					continue;
				this.elements.put(element.name(), element);
			} catch (XMLException | XMLMarshalingException e) {
				throw new XMLException("xml exception while loading: " + f.getName(), e);
			} catch (IOException e) {
				throw new IOException("io exception while loading model file: " + f.getName(), e);
			}
		}
		return this;
	}

	public NodalNetlistParser loadElementStreams(Collection<InputStream> streams) throws XMLException, IOException {
		for (InputStream sin : streams) {
			try {
				XMLInputStream xin = new XMLInputStream(sin);
				NodalElement element = ELEMENT_XML_PARSER.unmarshall(xin, NodalElement.class);
				this.elements.put(element.name(), element);
			} catch (XMLException | XMLMarshalingException e) {
				throw new XMLException("xml exception while loading stream", e);
			} catch (IOException e) {
				throw new IOException("io exception while loading stream", e);
			}
		}
		return this;
	}
	
	public NodalNetwork parseNetlist(File file) throws IOException {
		return parseNetlist(new FileInputStream(file));
	}
	
	public NodalNetwork parseNetlist(InputStream stream) throws IOException {
		try {
			BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
			NodalNetwork network = new NodalNetwork();
			String line;
			while ((line = bin.readLine()) != null) {
				try {
					parseNetlistLine(network, line);
				} catch (IllegalArgumentException e) {
					throw new IOException("encountered invalid netlist line while parsing", e);
				}
			}
			bin.close();
			return network;
		} catch (IOException e) {
			throw new IOException("io exception while reading netlist stream", e);
		}
	}
	
	public NodalNetwork parseNetlist(String netlist) throws IllegalArgumentException {
		return parseNetlist(netlist.lines().toArray(String[]::new));
	}
	
	public NodalNetwork parseNetlist(String... lines) throws IllegalArgumentException {
		NodalNetwork network = new NodalNetwork();
		int lnr = 1;
		for (String line : lines) {
			try {
				parseNetlistLine(network, line);
				lnr++;
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("error parsing netlist line: " + lnr, e);
			}
		}
		return network;
	}
	
	public NodalNetlistParser parseNetlistLine(NodalNetwork network, String line) throws IllegalArgumentException {
		if (line.isBlank() || line.startsWith("\\") || line.startsWith("//") || line.startsWith("*"))
			return this;
		
		String[] segments = line.split("\\s");
		
		if (segments.length <= 1)
			throw new IllegalArgumentException("line has not enough arguments: " + line);
		
		if (segments[0].equals("NZERO")) {
			network.setZeroNode(segments[1]);
			return this;
		}
		
		NodalElement element = this.elements.get(segments[0]);
		if (element == null)
			throw new IllegalArgumentException("line names an unknown element type: " + line);
		
		NodalElementState state = element.newInstance(segments[1]);
		
		for (int i = 2; i < segments.length; i++) {
			String[] s = segments[i].split("=");
			if (s.length != 2)
				throw new IllegalArgumentException("illegal element parameter key-value pair: " + segments[i]);
			
			if (element.hasNodeVariable(s[0], false)) {
				state.setNodeName(s[0], s[1]);
			} else {
				try {
					state.setParameter(s[0], Double.parseDouble(s[1]));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("not a valid number format for parameters value: " + segments[i]);
				}
			}
		}
		
		network.addElement(state);
		return this;
	}
	
}
