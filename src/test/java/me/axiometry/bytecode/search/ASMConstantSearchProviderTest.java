package me.axiometry.bytecode.search;

import static org.junit.Assert.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;

import me.axiometry.bytecode.*;

import org.junit.*;
import org.objectweb.asm.*;

public class ASMConstantSearchProviderTest {
	private abstract class SearchResultVerifier {
		private final String name;

		public SearchResultVerifier(String name) {
			this.name = name;
		}

		public abstract void verify(SearchResult result);

		public String getName() {
			return name;
		}
	}

	private byte[] classData;

	@Before
	public void setUp() throws Exception {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(51, Opcodes.ACC_PUBLIC, "Test1", null, "java/lang/Object", new String[0]);
		writer.visitField(Opcodes.ACC_PUBLIC, "field1", "LTest1;", null, null);
		MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "method1", "(LTest1;)V", null, new String[0]);
		method.visitVarInsn(Opcodes.ALOAD, 0);
		method.visitFieldInsn(Opcodes.GETFIELD, "Test1", "field1", "LTest1;");
		method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "Test1", "method1", "(LTest1;)V", false);
		method.visitInsn(Opcodes.RETURN);
		method.visitEnd();
		writer.visitEnd();
		classData = writer.toByteArray();
	}

	@Test
	public void testSearch() {
		ClassSource source = new SingleClassSource("Test1", classData);
		ASMConstantSearchProvider searchProvider = new ASMConstantSearchProvider();

		searchProvider.addSource(source);
		assertEquals(1, searchProvider.getSources().length);
		assertEquals(source, searchProvider.getSources()[0]);

		searchProvider.removeSource(source);
		assertEquals(0, searchProvider.getSources().length);

		searchProvider.addSource(source);
		assertEquals(1, searchProvider.getSources().length);
		assertEquals(source, searchProvider.getSources()[0]);

		final SearchFunction function = SearchFunctions.classReference("Test1");
		ConstantSearchProvider.ConstantSearcher search = searchProvider.search(function);

		List<SearchResultVerifier> verifiers = new ArrayList<>();
		verifiers.add(new SearchResultVerifier("Class Result of Class") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(ClassSearchResult.class, result.getClass());
				ClassSearchResult classResult = (ClassSearchResult) result;
				assertEquals("Test1", classResult.getClassName());
			}
		});
		verifiers.add(new SearchResultVerifier("Field Result of Field in Class") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(FieldSearchResult.class, result.getClass());
				FieldSearchResult fieldResult = (FieldSearchResult) result;
				assertEquals("Test1", fieldResult.getClassName());
				assertEquals("field1", fieldResult.getFieldName());
				assertEquals("LTest1;", fieldResult.getFieldDescriptor());
			}
		});
		verifiers.add(new SearchResultVerifier("Method Result of Method in Class") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(MethodSearchResult.class, result.getClass());
				MethodSearchResult methodResult = (MethodSearchResult) result;
				assertEquals("Test1", methodResult.getClassName());
				assertEquals("method1", methodResult.getMethodName());
				assertEquals("(LTest1;)V", methodResult.getMethodDescriptor());
			}
		});
		verifiers.add(new SearchResultVerifier("Method Instruction Result of Field Instruction in Method in Class (Field Owner or Type)") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(MethodInstructionSearchResult.class, result.getClass());
				MethodInstructionSearchResult methodResult = (MethodInstructionSearchResult) result;
				assertEquals("Test1", methodResult.getClassName());
				assertEquals("method1", methodResult.getMethodName());
				assertEquals("(LTest1;)V", methodResult.getMethodDescriptor());
				assertEquals(Opcodes.GETFIELD, methodResult.getOpcode());
			}
		});
		verifiers.add(new SearchResultVerifier("Method Instruction Result of Field Instruction in Method in Class (Field Owner or Type)") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(MethodInstructionSearchResult.class, result.getClass());
				MethodInstructionSearchResult methodResult = (MethodInstructionSearchResult) result;
				assertEquals("Test1", methodResult.getClassName());
				assertEquals("method1", methodResult.getMethodName());
				assertEquals("(LTest1;)V", methodResult.getMethodDescriptor());
				assertEquals(Opcodes.GETFIELD, methodResult.getOpcode());
			}
		});
		verifiers.add(new SearchResultVerifier("Method Instruction Result of Method Instruction in Method in Class (Method Owner or Argument)") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(MethodInstructionSearchResult.class, result.getClass());
				MethodInstructionSearchResult methodResult = (MethodInstructionSearchResult) result;
				assertEquals("Test1", methodResult.getClassName());
				assertEquals("method1", methodResult.getMethodName());
				assertEquals("(LTest1;)V", methodResult.getMethodDescriptor());
				assertEquals(Opcodes.INVOKEVIRTUAL, methodResult.getOpcode());
			}
		});
		verifiers.add(new SearchResultVerifier("Method Instruction Result of Method Instruction in Method in Class (Method Owner or Argument)") {
			@Override
			public void verify(SearchResult result) {
				assertEquals(ClassReference.class, result.getConstant().getClass());
				assertEquals("Test1", ((ClassReference) result.getConstant()).getClassName());

				assertEquals(MethodInstructionSearchResult.class, result.getClass());
				MethodInstructionSearchResult methodResult = (MethodInstructionSearchResult) result;
				assertEquals("Test1", methodResult.getClassName());
				assertEquals("method1", methodResult.getMethodName());
				assertEquals("(LTest1;)V", methodResult.getMethodDescriptor());
				assertEquals(Opcodes.INVOKEVIRTUAL, methodResult.getOpcode());
			}
		});

		SearchResult result;
		for(result = search.next(); result != null && verifiers.size() > 0; result = search.next()) {
			assertNotNull(result);
			assertNotNull(result.getConstant());
			assertNotNull(result.getFunction());
			assertEquals(function, result.getFunction());

			SearchResultVerifier successfulVerifier = null;
			Iterator<SearchResultVerifier> iterator = verifiers.iterator();
			while(iterator.hasNext()) {
				SearchResultVerifier verifier = iterator.next();
				try {
					verifier.verify(result);
					successfulVerifier = verifier;
					iterator.remove();
					break;
				} catch(Throwable ignored) {}
			}
			if(successfulVerifier == null)
				fail("No verifier matched " + result.getClass().getName());
		}
		assertNull(result);
		for(SearchResultVerifier verifier : verifiers)
			fail("Verifier failed: " + verifier.getName());
	}

	@Test(timeout = 30000)
	public void testStopOnGC() {
		ClassSource source = new SingleClassSource("Test1", classData);
		ASMConstantSearchProvider searchProvider = new ASMConstantSearchProvider();
		searchProvider.addSource(source);

		SearchFunction function = SearchFunctions.classReference("Test1");
		ConstantSearchProvider.ConstantSearcher search = searchProvider.search(function);
		WeakReference<ConstantSearchProvider.ConstantSearcher> searchReference = new WeakReference<>(search);

		Object thread;
		Thread actualThread;
		try {
			Field threadField = search.getClass().getDeclaredField("thread");
			threadField.setAccessible(true);
			thread = threadField.get(search);

			assertNotNull(thread);

			Field actualThreadField = thread.getClass().getDeclaredField("thread");
			actualThreadField.setAccessible(true);
			actualThread = (Thread) actualThreadField.get(thread);

			assertNotNull(actualThread);
			assertTrue(actualThread.isAlive());
		} catch(Exception exception) {
			throw new AssertionError("Reflecting thread field from ASM search failed", exception);
		}

		assertNotNull(search.next());
		search = null;
		System.gc();

		while(actualThread.isAlive()) {
			try {
				Thread.sleep(500);
			} catch(InterruptedException exception) {}
		}

		assertNull(searchReference.get());
		assertFalse(actualThread.isAlive());
	}

	@After
	public void tearDown() throws Exception {
		classData = null;
	}
}
