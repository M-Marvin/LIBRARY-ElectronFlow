package de.m_marvin.tvnlnna.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import de.m_marvin.basicxml.XmlException;
import de.m_marvin.basicxml.XmlReader;
import de.m_marvin.basicxml.XmlWriter;
import de.m_marvin.basicxml.dom.XmlDOM;
import de.m_marvin.basicxml.dom.XmlElement;
import de.m_marvin.basicxml.dom.XmlNamed;
import de.m_marvin.basicxml.dom.XmlPrimitive;
import de.m_marvin.basicxml.marshaling.XmlMarshalingException;
import de.m_marvin.basicxml.marshaling.XmlUnmarshaler;
import de.m_marvin.tvnlnna.NodalModelLoadException;
import de.m_marvin.tvnlnna.nodal.INodalElement;
import de.m_marvin.tvnlnna.nodal.NodalElement;

public class NodalModelLoader {
	
	public static class SingleSourceByteCodeLoader extends ClassLoader {
		
		private final Map<String, Class<?>> classCache = new LinkedHashMap<String, Class<?>>();
		
		public static SingleSourceByteCodeLoader fromBase64(String base64) throws IOException {
			try {
				DataInputStream compressedBytecode = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64))));
				int numberOfClasses = compressedBytecode.readUnsignedByte();
				Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
				for (int i = 0; i < numberOfClasses; i++) {
					String name = compressedBytecode.readUTF();
					byte[] bytecode = compressedBytecode.readNBytes(compressedBytecode.readInt());
					classes.put(name, bytecode);
				}
				return new SingleSourceByteCodeLoader(classes);
			} catch (ClassFormatError | NoClassDefFoundError | SecurityException | IndexOutOfBoundsException e) {
				throw new IOException("unable to parse one or more classes", e);
			}
		}
		
		public SingleSourceByteCodeLoader(Map<String, byte[]> classes) {
			for (var entry : classes.entrySet()) {
				this.classCache.put(entry.getKey(), defineClass(entry.getKey(), entry.getValue(), 0, entry.getValue().length));
			}
		}
		
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			return this.classCache.get(name);
		}
		
		public String getFirstClassName() {
			return this.classCache.keySet().stream().findFirst().orElse(null);
		}
		
	}
	
	private static final XmlDOM XML_DOM = new XmlDOM();
	private static final XmlUnmarshaler XML_UNMARSHALER = new XmlUnmarshaler(true, NodalElement.class);
	
	public static INodalElement loadModel(InputStream modelStream) throws NodalModelLoadException {
		
		try {
			XmlNamed dom = XML_DOM.read(new XmlReader(modelStream));
			if (dom == null)
				throw new XmlException("not a valid XML file");
			
			// check for a definition of a custom class
			NodalElement elementInstance = null;
			XmlElement classTag;
			if ((classTag = dom.getValue().getAsTag().getEntry("class")) != null && classTag.isTag()) {
				
				Optional<String> classEncoded = classTag.getAsTag().getInnerElements()
					.filter(XmlNamed::isInnerText)
					.map(XmlNamed::getValue)
					.map(XmlElement::getAsPrimitive)
					.map(XmlPrimitive::getAsString)
					.map(s -> s.replaceAll("\\s", ""))
					.reduce(String::concat);
				
				if (classEncoded.isEmpty())
					throw new XmlException("empty model <class> tag");
				
				try {
					SingleSourceByteCodeLoader modelClassLoader = SingleSourceByteCodeLoader.fromBase64(classEncoded.get());
					String className = modelClassLoader.getFirstClassName();
					if (className == null)
						throw new NodalModelLoadException("no class defined within <class> tag");
					Class<?> clazz = modelClassLoader.loadClass(className);
					if (!NodalElement.class.isAssignableFrom(clazz))
						throw new NodalModelLoadException("model class is not compatible with super class: " + clazz.getName() + " not super " + NodalElement.class.getName());
					elementInstance = NodalElement.class.cast(clazz.getConstructor().newInstance());
				} catch (ClassNotFoundException e) {
					throw new NodalModelLoadException("unable to load model class: " + e.getMessage(), e);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new NodalModelLoadException("unable to invoke model class constructor: " + e.getMessage(), e);
				} catch (NoSuchMethodException e) {
					throw new NodalModelLoadException("model class is missing default constructor: " + e.getMessage(), e);
				}
				
			} else if (classTag != null) {
				throw new XmlException("illegal declaration of model <class> tag");
			}
			
			// parse model instance configuration
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			XML_DOM.write(new XmlWriter(buffer), dom);
			return XML_UNMARSHALER.unmarshall(new XmlReader(new ByteArrayInputStream(buffer.toByteArray())), elementInstance, NodalElement.class);
			
		} catch (IOException | XmlException | XmlMarshalingException e) {
			throw new NodalModelLoadException("exception while parsing model file: " + e.getMessage(), e);
		}
		
	}
	
}
