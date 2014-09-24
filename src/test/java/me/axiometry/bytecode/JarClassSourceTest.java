package me.axiometry.bytecode;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.ZipException;

import org.junit.*;
import org.objectweb.asm.*;

public class JarClassSourceTest {
	private byte[] jarData, class1Data, class2Data;

	@Before
	public void setUp() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(51, Opcodes.ACC_PUBLIC, "Test1", null, "java/lang/Object", new String[0]);
		writer.visitEnd();
		class1Data = writer.toByteArray();
		writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(51, Opcodes.ACC_PUBLIC, "Test2", null, "java/lang/Object", new String[0]);
		writer.visitEnd();
		class2Data = writer.toByteArray();

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		try(JarOutputStream out = new JarOutputStream(byteOut)) {
			out.putNextEntry(new JarEntry("Test1.class"));
			out.write(class1Data);
			out.closeEntry();
			out.putNextEntry(new JarEntry("Test2.class"));
			out.write(class2Data);
			out.closeEntry();
		} catch(IOException exception) {
			exception.printStackTrace();
			fail("Error occurred");
			return;
		}
		jarData = byteOut.toByteArray();
	}

	@Test
	public void testConstructorInputStream() {
		JarClassSource source;
		try {
			source = new JarClassSource(new ByteArrayInputStream(jarData));
		} catch(IOException exception) {
			exception.printStackTrace();
			fail("Error occurred");
			return;
		}
		testClasses(source);
	}

	@Test
	public void testConstructorByteArray() {
		JarClassSource source;
		try {
			source = new JarClassSource(jarData);
		} catch(ZipException exception) {
			exception.printStackTrace();
			fail("Error occurred");
			return;
		}
		testClasses(source);
	}

	private void testClasses(JarClassSource source) {
		Map<String, byte[]> classes = source.getClasses();

		assertNotNull("Classes were null", classes);
		assertEquals("Wrong number of classes!", 2, classes.size());

		assertTrue("Test1 not found in classes", classes.containsKey("Test1"));
		assertTrue("Test2 not found in classes", classes.containsKey("Test2"));

		assertArrayEquals("Test1 class data did not match", class1Data, classes.get("Test1"));
		assertArrayEquals("Test2 class data did not match", class2Data, classes.get("Test2"));
	}

	@After
	public void tearDown() {
		jarData = null;
		class1Data = null;
		class2Data = null;
	}
}
