package de.m_marvin.tvnlnna.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.Optional;

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
	
	public static class OpenBytecodeLoader extends ClassLoader {
		
		public static final OpenBytecodeLoader LOADER = new OpenBytecodeLoader(OpenBytecodeLoader.class.getClassLoader());
		
		private OpenBytecodeLoader(ClassLoader parent) {
			super(parent);
		}
		
		public Class<?> defineClass(byte[] byteCode) throws ClassFormatError, NoClassDefFoundError, SecurityException, IndexOutOfBoundsException {
			Class<?> clazz = defineClass(null, byteCode, 0, byteCode.length);
			resolveClass(clazz);
			return clazz;
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
					byte[] classDecoded = Base64.getDecoder().decode(classEncoded.get());
					Class<?> clazz = OpenBytecodeLoader.LOADER.defineClass(classDecoded);
					if (!NodalElement.class.isAssignableFrom(clazz))
						throw new NodalModelLoadException("model class is not compatible with super class: " + clazz.getName() + " not super " + NodalElement.class.getName());
					elementInstance = NodalElement.class.cast(clazz.getConstructor().newInstance());
				} catch (ClassFormatError | NoClassDefFoundError | SecurityException | IndexOutOfBoundsException e) {
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
