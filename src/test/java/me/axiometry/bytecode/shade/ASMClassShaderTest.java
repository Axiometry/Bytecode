package me.axiometry.bytecode.shade;

import me.axiometry.bytecode.*;

import org.junit.*;
import org.objectweb.asm.*;

public class ASMClassShaderTest {
	private ASMClassShader shader;

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

		byte[] classData = writer.toByteArray();
		//SingleClassSource source = new SingleClassSource("Test1", classData);
		ClassSource source = new ClassLoaderClassSource(getClass().getClassLoader(), true);

		shader = new ASMClassShader();
		shader.addSource(source);
	}

	@Test
	public void testShade() {
		ShadeFunction function = (Identifier identifier) -> {
			//if(identifier instanceof ClassIdentifier && ((ClassIdentifier) identifier).getClassName().equals("Test1"))
			//	return new ClassIdentifier("Hacked");
			if(identifier instanceof ClassIdentifier && identifier.getClassName().equals("java.lang.Object"))
				return new ClassIdentifier("java.lang.Hacked");
			if(identifier instanceof ClassIdentifier && identifier.getClassName().equals("java.lang.String"))
				return new ClassIdentifier("java.lang.Stupid");
			return identifier;
		};
		shader.shade(function);
	}

	@After
	public void tearDown() throws Exception {
		shader = null;
	}
}
