package test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class ModelClassCompiler {

	public static void compileFile(File file) {
		
		try {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler(); 
			Iterable<JavaFileObject> javaSource = Collections.singleton(new JavaSourceFileObject(file));
			List<String> options = List.of("-classpath", System.getProperty("java.class.path"));
			InMemoryFileManager fileManager = new InMemoryFileManager(compiler.getStandardFileManager(null, null, null));
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			CompilationTask task = compiler.getTask(
					new PrintWriter(System.out), 
					fileManager, 
					diagnostics,
					options, 
					null, 
					javaSource
			);
			if (task.call()) {
				ByteArrayOutputStream compressedBytecode = new ByteArrayOutputStream();
				DataOutputStream writer = new DataOutputStream(new GZIPOutputStream(compressedBytecode));
				writer.writeByte(fileManager.getCompiledClasses().size());
				for (String className : fileManager.getCompiledClasses().stream().sorted().toList()) {
					byte[] bytecode = fileManager.getClassBytes(className);
					writer.writeUTF(className);
					writer.writeInt(bytecode.length);
					writer.write(bytecode);
				}
				writer.close();
				String base64 = Base64.getEncoder().encodeToString(compressedBytecode.toByteArray());
				System.out.println(base64);
				System.exit(0);
			} else {
				diagnostics.getDiagnostics().forEach(diag -> System.err.println(diag.toString()));
				System.exit(-1);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	public static class JavaSourceFileObject extends SimpleJavaFileObject {

		public JavaSourceFileObject(File file) {
			super(file.toURI(), Kind.SOURCE);
		}
		
		@Override
		public InputStream openInputStream() throws IOException {
			return this.uri.toURL().openStream();
		}
		
		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			InputStream stream = openInputStream();
			String str = new String(stream.readAllBytes());
			stream.close();
			return str;
		}
		
	}
	
	public static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

		private final Map<String, BytesJavaClass> name2classMap= new LinkedHashMap<>();
		
		public InMemoryFileManager(JavaFileManager fileManager) {
			super(fileManager);
		}
		
		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
			BytesJavaClass classObject = new BytesJavaClass(className, kind);
			this.name2classMap.put(className, classObject);
			return classObject;
		}
		
		public Set<String> getCompiledClasses() {
			return this.name2classMap.keySet();
		}
		
		public byte[] getClassBytes(String name) {
			return this.name2classMap.containsKey(name) ? this.name2classMap.get(name).getClassBytes() : null;
		}
		
	}
	
	public static class BytesJavaClass extends SimpleJavaFileObject {
		
		private ByteArrayOutputStream buffer;
		
		public BytesJavaClass(String name, Kind kind) {
			super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
			this.buffer = new ByteArrayOutputStream();
		}
		
		@Override
		public OutputStream openOutputStream() throws IOException {
			return this.buffer;
		}
		
		public byte[] getClassBytes() {
			return this.buffer.toByteArray();
		}
		
	}
	
}
