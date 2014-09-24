package me.axiometry.bytecode;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

public final class ClassLoaderClassSource implements ClassSource {
	private final Map<String, byte[]> classes;

	public ClassLoaderClassSource(ClassLoader classLoader) throws IOException, IllegalArgumentException, IllegalAccessException {
		this(classLoader, true);
	}

	@SuppressWarnings("unchecked")
	public ClassLoaderClassSource(ClassLoader classLoader, boolean ignoreUnreadableClasses) throws IOException,
			IllegalArgumentException,
			IllegalAccessException {
		Field classLoaderClassesField;
		try {
			classLoaderClassesField = ClassLoader.class.getDeclaredField("classes");
		} catch(NoSuchFieldException exception) {
			throw new RuntimeException("Field 'classes' is not present in ClassLoader (???)", exception);
		}
		classLoaderClassesField.setAccessible(true);

		ImmutableMap.Builder<String, byte[]> classes = ImmutableMap.builder();
		List<Class<?>> classLoaderClasses;
		try {
			classLoaderClasses = Arrays.asList(((Vector<Class<?>>) classLoaderClassesField.get(classLoader)).toArray(new Class<?>[0]));
		} catch(IllegalArgumentException | IllegalAccessException exception) {
			throw exception;
		}
		for(Class<?> c : classLoaderClasses) {
			String name = c.getName();
			String resourceName = name.replace('.', '/') + ".class";
			try(InputStream stream = classLoader.getResourceAsStream(resourceName)) {
				if(stream == null)
					if(!ignoreUnreadableClasses)
						throw new NullPointerException();
					else
						continue;
				classes.put(name, ByteStreams.toByteArray(stream));
			} catch(IOException exception) {
				if(!ignoreUnreadableClasses)
					throw exception;
			}
		}
		this.classes = classes.build();
	}

	@Override
	public Map<String, byte[]> getClasses() {
		ImmutableMap.Builder<String, byte[]> builder = ImmutableMap.builder();
		for(Map.Entry<String, byte[]> entry : classes.entrySet())
			builder.put(entry.getKey(), entry.getValue().clone());
		return builder.build();
	}
}
