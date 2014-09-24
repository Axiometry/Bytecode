package me.axiometry.bytecode;

import java.io.*;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.ZipException;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

public class JarClassSource implements ClassSource {
	private final Map<String, byte[]> classes;

	public JarClassSource(InputStream in) throws IOException {
		classes = readClasses(in);
	}

	public JarClassSource(byte[] data) throws ZipException {
		try {
			classes = readClasses(new ByteArrayInputStream(data));
		} catch(ZipException exception) {
			throw exception;
		} catch(IOException exception) {
			// Should not occur
			throw new RuntimeException("IO error parsing jar", exception);
		}
	}

	private static Map<String, byte[]> readClasses(InputStream stream) throws IOException {
		ImmutableMap.Builder<String, byte[]> builder = ImmutableMap.builder();

		try(JarInputStream jarStream = new JarInputStream(stream)) {
			for(JarEntry entry = jarStream.getNextJarEntry(); entry != null; entry = jarStream.getNextJarEntry()) {
				String name = entry.getName();
				if(!name.endsWith(".class"))
					continue;

				name = name.replaceAll("\\.class$", "").replace('/', '.');
				byte[] data = ByteStreams.toByteArray(jarStream);

				builder.put(name, data);
			}
		}

		return builder.build();
	}

	@Override
	public Map<String, byte[]> getClasses() {
		ImmutableMap.Builder<String, byte[]> builder = ImmutableMap.builder();
		for(Map.Entry<String, byte[]> entry : classes.entrySet())
			builder.put(entry.getKey(), entry.getValue().clone());
		return builder.build();
	}

}
