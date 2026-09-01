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
import tvnlnna.nodal.INodalElement;
import tvnlnna.nodal.NodalElement;
import tvnlnna.nodal.NodalElementState;
import tvnlnna.nodal.NodalNetwork;

/**
 * An netlist parser which can read netlists such as those produced by {@link NodalNetwork#toString()} to an new {@link NodalNetwork} instance.
 * The required {@link INodalElement} can be provided during construction of the parser either as a XML files or custom implementations.
 */
public class NodalNetlistParser {
	
	public static final XMLUnmarshaler ELEMENT_XML_PARSER = new XMLUnmarshaler(true, NodalElement.class);
	
	private final Map<String, INodalElement> elements = new HashMap<>();
	
	private NodalNetlistParser() {}
	
	/**
	 * Creates a new parser without any element definitions registered.
	 * @return a new parser instance
	 */
	public static NodalNetlistParser empty() {
		return new NodalNetlistParser();
	}
	
	/**
	 * Adds the supplied element definitions to this parsers internal registry.
	 * Duplicate names result in only the last element in the list with the given name to be registered.
	 * @param elements the element definitions to register
	 * @return the same parser instance, with the new element definitions registered
	 */
	public NodalNetlistParser addElements(Collection<INodalElement> elements) {
		for (var e : elements)
			this.elements.put(e.name(), e);
		return this;
	}
	
	/**
	 * Loads element definitions from the XML files in the supplied folder.
	 * The application has to ensure that the folder only contains valid element definition files.
	 * The parser will abort on the first invalid file and throw an exception.
	 * @param folder the folder to load the files from
	 * @return the same parser instance, with the new element definitions registered
	 * @throws XMLException if an XML file in the folder contains an invalid XML syntax
	 * @throws IOException if an file in the folder could not be parsed because of an IO error
	 */
	public NodalNetlistParser loadElements(File folder) throws XMLException, IOException {
		return loadElements(Stream.of(folder.listFiles()).filter(f -> f.isFile()).toList());
	}

	/**
	 * Loads element definitions from the supplied XML files
	 * @param files the files to load
	 * @return the same parser instance, with the new element definitions registered
	 * @throws XMLException if an XML file in the list contains an invalid XML syntax
	 * @throws IOException if an file in the list could not be parsed because of an IO error
	 */
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

	/**
	 * Loads element definitions from the supplied XML streams
	 * @param streams the streams to load
	 * @return the same parser instance, with the new element definitions registered
	 * @throws XMLException if an XML stream in the list contains an invalid XML syntax
	 * @throws IOException if an stream in the list could not be parsed because of an IO error
	 */
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
	
	/**
	 * Parses the netlist file to an new {@link NodalNetwork} instance.
	 * @param file the file to load the netlist string from
	 * @return a new netlist instance
	 * @throws IOException if an IO error occurs during reading from the file
	 */
	public NodalNetwork<String, String> parseNetlist(File file) throws IOException {
		return parseNetlist(new FileInputStream(file));
	}
	
	/**
	 * Parses the netlist stream to an new {@link NodalNetwork} instance.
	 * @param stream the stream to load the netlist string from
	 * @return a new netlist instance
	 * @throws IOException if an IO error occurs during reading from the stream
	 */
	public NodalNetwork<String, String> parseNetlist(InputStream stream) throws IOException {
		try {
			BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
			NodalNetwork<String, String> network = new NodalNetwork<String, String>();
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
	
	/**
	 * Parses the netlist string to an new {@link NodalNetwork} instance.
	 * @param netlist the netlist string to parse
	 * @return a new netlist instance
	 * @throws IllegalArgumentException if any invalid lines in the netlist are encountered
	 */
	public NodalNetwork<String, String> parseNetlist(String netlist) throws IllegalArgumentException {
		return parseNetlist(netlist.lines().toArray(String[]::new));
	}

	/**
	 * Parses the netlist line strings to an new {@link NodalNetwork} instance.
	 * @param lines the netlist lines to parse
	 * @return a new netlist instance
	 * @throws IllegalArgumentException if any invalid lines in the netlist are encountered
	 */
	public NodalNetwork<String, String> parseNetlist(String... lines) throws IllegalArgumentException {
		NodalNetwork<String, String> network = new NodalNetwork<String, String>();
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
	
	/**
	 * Parses an single line of an netlist and appends any new elements or settings to the supplied network.
	 * @param network the nodal network to apply the changes to
	 * @param line the netlist line to parse
	 * @return the parser instance without any changes
	 * @throws IllegalArgumentException if the line is invalid because of an syntax error or an invalid element or parameter identifier
	 */
	public NodalNetlistParser parseNetlistLine(NodalNetwork<String, String> network, String line) throws IllegalArgumentException {
		if (line.isBlank() || line.startsWith("\\") || line.startsWith("//") || line.startsWith("*"))
			return this;
		
		String[] segments = line.split("\\s");
		
		if (segments.length <= 1)
			throw new IllegalArgumentException("line has not enough arguments: " + line);
		
		if (segments[0].equals("NZERO")) {
			network.setZeroNode(segments[1]);
			return this;
		}
		
		INodalElement element = this.elements.get(segments[0]);
		if (element == null)
			throw new IllegalArgumentException("line names an unknown element type: " + line);
		
		NodalElementState<String, String> state = element.newInstance(segments[1]);
		
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
