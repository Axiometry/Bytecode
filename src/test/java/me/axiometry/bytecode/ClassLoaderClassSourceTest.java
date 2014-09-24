package me.axiometry.bytecode;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.*;
import org.objectweb.asm.*;

import com.google.common.collect.ImmutableList;

public class ClassLoaderClassSourceTest {
	private static final class DirectClassLoader extends ClassLoader {
		private final Map<String, byte[]> classes;

		public DirectClassLoader() {
			classes = new ConcurrentHashMap<>();
		}

		public void addClass(String name, byte[] data) {
			classes.put(name, data.clone());
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] data = classes.get(name);
			if(data != null)
				return defineClass(name, data, 0, data.length);
			throw new ClassNotFoundException();
		}
		@Override
		protected URL findResource(String name) {
			if(!name.endsWith(".class"))
				return null;
			name = name.replaceFirst("^/", "").replaceFirst("\\.class$", "").replace('/', '.');

			final byte[] data = classes.get(name);
			if(data != null) {
				try {
					return new URL("raw", null, -1, name.replace('.', '/').concat(".class"), new URLStreamHandler() {

						@Override
						protected URLConnection openConnection(URL u) throws IOException {
							return new URLConnection(u) {
								private ByteArrayInputStream in = new ByteArrayInputStream(data);

								@Override
								public void connect() throws IOException {
								}

								@Override
								public InputStream getInputStream() throws IOException {
									return in;
								}
							};
						}
					});
				} catch(MalformedURLException exception) {
					throw new AssertionError("Error creating URL", exception);
				}
			}
			return null;
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			URL resource = findResource(name);
			if(resource != null)
				return Collections.enumeration(ImmutableList.of(resource));
			return Collections.emptyEnumeration();
		}
	}

	private ClassLoader classLoader;
	private byte[] class1Data, class2Data;

	@Before
	public void setUp() throws Exception {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(51, Opcodes.ACC_PUBLIC, "Test1", null, "java/lang/Object", new String[0]);
		writer.visitEnd();
		class1Data = writer.toByteArray();

		writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(51, Opcodes.ACC_PUBLIC, "Test2", null, "java/lang/Object", new String[0]);
		writer.visitEnd();
		class2Data = writer.toByteArray();

		DirectClassLoader classLoader = new DirectClassLoader();
		classLoader.addClass("Test1", class1Data);
		classLoader.addClass("Test2", class2Data);

		classLoader.loadClass("Test1");
		classLoader.loadClass("Test2");
		this.classLoader = classLoader;
	}
	@Test
	public void testConstructor() {
		ClassLoaderClassSource source;
		try {
			source = new ClassLoaderClassSource(classLoader);
		} catch(Exception exception) {
			throw new AssertionError("Error occurred", exception);
		}
		testClasses(source);
	}

	@Test
	public void testConstructorWithFlag() {
		ClassLoaderClassSource source;
		try {
			source = new ClassLoaderClassSource(classLoader, false);
		} catch(Exception exception) {
			throw new AssertionError("Error occurred", exception);
		}
		testClasses(source);
	}

	private void testClasses(ClassLoaderClassSource source) {
		Map<String, byte[]> classes = source.getClasses();

		assertNotNull(classes);
		assertEquals(2, classes.size());

		assertTrue(classes.containsKey("Test1"));
		assertTrue(classes.containsKey("Test2"));

		assertArrayEquals(class1Data, classes.get("Test1"));
		assertArrayEquals(class2Data, classes.get("Test2"));
	}

	@After
	public void tearDown() throws Exception {
		classLoader = null;
		class1Data = null;
		class2Data = null;
	}
}
