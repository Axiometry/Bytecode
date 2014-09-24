package me.axiometry.bytecode;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Map;

import org.junit.*;
import org.objectweb.asm.*;

public class SingleClassSourceTest {
	private byte[] classData;

	@Before
	public void setUp() throws Exception {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(51, Opcodes.ACC_PUBLIC, "Test", null, "java/lang/Object", new String[0]);
		writer.visitEnd();

		classData = writer.toByteArray();
	}

	@Test
	public void testConstructorInputStream() {
		SingleClassSource source;
		try {
			source = new SingleClassSource("Test", new ByteArrayInputStream(classData));
		} catch(IOException exception) {
			exception.printStackTrace();
			fail("Error occurred");
			return;
		}
		testClasses(source);
	}

	@Test
	public void testConstructorByteArray() {
		SingleClassSource source = new SingleClassSource("Test", classData);
		testClasses(source);
	}

	private void testClasses(SingleClassSource source) {
		Map<String, byte[]> classes = source.getClasses();
		assertNotNull("Classes were null", classes);
		assertEquals("Wrong number of classes!", 1, classes.size());
		assertTrue("Test not found in classes", classes.containsKey("Test"));
		assertArrayEquals("Test class data did not match", classData, classes.get("Test"));
	}

	@After
	public void tearDown() throws Exception {
		classData = null;
	}
}
