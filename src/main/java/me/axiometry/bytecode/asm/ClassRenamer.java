package me.axiometry.bytecode.asm;

import java.util.*;

import org.objectweb.asm.*;

class ClassRenamer extends ClassVisitor implements Opcodes {
	private Set<String> oldNames;
	private final String newName;

	public ClassRenamer(ClassVisitor cv, Set<String> oldNames, String newName) {
		super(ASM5, cv);
		this.oldNames = oldNames;
		this.newName = newName;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		oldNames.add(name);
		cv.visit(version, ACC_PUBLIC, newName, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, fix(desc), fix(signature), exceptions);
		if(mv != null && (access & ACC_ABSTRACT) == 0) {
			mv = new MethodRenamer(mv);
		}
		return mv;
	}

	class MethodRenamer extends MethodVisitor {

		public MethodRenamer(final MethodVisitor mv) {
			super(ASM5, mv);
		}

		@Override
		public void visitTypeInsn(int i, String s) {
			if(oldNames.contains(s)) {
				s = newName;
			}
			mv.visitTypeInsn(i, s);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			if(oldNames.contains(owner)) {
				mv.visitFieldInsn(opcode, newName, name, fix(desc));
			} else {
				mv.visitFieldInsn(opcode, owner, name, fix(desc));
			}
		}

		@Override
		@SuppressWarnings("deprecation")
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			if(oldNames.contains(owner)) {
				mv.visitMethodInsn(opcode, newName, name, fix(desc));
			} else {
				mv.visitMethodInsn(opcode, owner, name, fix(desc));
			}
		}
	}

	private String fix(String s) {
		if(s == null)
			return null;
		Iterator<String> it = oldNames.iterator();
		String name;
		while(it.hasNext()) {
			name = it.next();
			if(s.indexOf(name) != -1) {
				s = s.replaceAll(name, newName);
			}
		}
		return s;
	}
}