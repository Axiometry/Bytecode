package me.axiometry.bytecode.search;

import java.util.Map;

import me.axiometry.bytecode.ClassSource;

import org.objectweb.asm.*;

import com.google.common.collect.ImmutableMap;

public class ASMConstantSearchProvider extends AbstractConstantSearchProvider {
	private static final Class<?>[] ACCEPTED_CONSTANT_TYPES = { String.class, Integer.class, Long.class, Short.class, Double.class, Float.class, Byte.class };

	@Override
	public ConstantSearcher search(SearchFunction... functions) {
		return new ASMConstantSearcher(functions);
	}

	// hackish coroutine implementation (Thread behind the scenes)
	private final class ASMConstantSearcher implements ConstantSearcher {
		private final SearchFunction[] functions;
		private final ClassSource[] sources;

		private final ASMConstantSearchThread thread;

		public ASMConstantSearcher(SearchFunction... functions) {
			this.functions = functions.clone();
			sources = ASMConstantSearchProvider.this.getSources();

			thread = new ASMConstantSearchThread(this.functions, sources);
		}

		@Override
		public SearchResult next() {
			return thread.next();
		}

		@Override
		protected void finalize() throws Throwable {
			thread.stop();
		}

		@Override
		public SearchFunction[] getFunctions() {
			return functions;
		}

		@Override
		public ClassSource[] getSources() {
			return sources;
		}
	}

	private static final class ASMConstantSearchThread implements Runnable {
		private enum SearchState {
			WAITING, SEARCHING, FOUND, NOT_FOUND, STOPPED
		}

		@SuppressWarnings("serial")
		private static class SearchStoppedException extends RuntimeException {
		}

		private final Thread thread;

		private final SearchFunction[] functions;
		private final ClassSource[] sources;

		private SearchResult result = null;
		private SearchState state = SearchState.WAITING;

		public ASMConstantSearchThread(SearchFunction[] functions, ClassSource[] sources) {
			this.functions = functions;
			this.sources = sources;

			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}

		private SearchFunction apply(Object constant) {
			for(int i = 0; i < functions.length; i++) {
				SearchFunction function = functions[i];
				if(function.matches(constant))
					return function;
			}
			return null;
		}

		@Override
		public synchronized void run() {
			try {
				awaitNextSearch();

				for(final ClassSource source : sources) {
					Map<String, byte[]> classes = ImmutableMap.copyOf(source.getClasses());

					for(Map.Entry<String, byte[]> classData : classes.entrySet()) {
						final String className = classData.getKey();
						ClassReader reader = new ClassReader(classData.getValue());
						ClassVisitor visitor = createClassVisitor(source, className);
						reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

						if(state == SearchState.STOPPED)
							return;
					}

					if(state == SearchState.STOPPED)
						return;
				}

				state = SearchState.NOT_FOUND;
			} catch(SearchStoppedException exception) {
				result = null;

				Thread.interrupted();
			}
		}

		private ClassVisitor createClassVisitor(final ClassSource source, final String className) {
			return new ClassVisitor(Opcodes.ASM5) {
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					ClassReference ref = new ClassReference(className);

					SearchFunction function = apply(ref);
					if(function != null)
						resultFound(new ClassSearchResult(function, ref, source, className));
				}

				@Override
				public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
					{
						FieldReference ref = new FieldReference(className, name, desc);

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new FieldSearchResult(function, ref, source, className, name, desc));
					}
					{
						ClassReference ref = new ClassReference(Type.getType(desc).getClassName());

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new FieldSearchResult(function, ref, source, className, name, desc));
					}

					return super.visitField(access, name, desc, signature, value);
				}

				@Override
				public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
					{
						MethodReference ref = new MethodReference(className, name, desc);

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodSearchResult(function, ref, source, className, name, desc));
					}
					{
						ClassReference ref = new ClassReference(Type.getReturnType(desc).getClassName());

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodSearchResult(function, ref, source, className, name, desc));
					}
					for(Type argumentType : Type.getArgumentTypes(desc)) {
						ClassReference ref = new ClassReference(argumentType.getClassName());

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodSearchResult(function, ref, source, className, name, desc));
					}

					return createMethodVisitor(source, className, name, desc);
				}
			};
		}

		public MethodVisitor createMethodVisitor(final ClassSource source, final String className, final String methodName, final String methodDescriptor) {
			return new MethodVisitor(Opcodes.ASM5) {
				@Override
				public void visitLdcInsn(Object constant) {
					if(constant == null)
						return;
					Class<?> constantType = constant.getClass();
					for(Class<?> type : ACCEPTED_CONSTANT_TYPES) {
						if(type.equals(constantType)) {
							SearchFunction function = apply(constant);
							if(function != null)
								resultFound(new MethodInstructionSearchResult(function, constant, source, className, methodName, methodDescriptor, Opcodes.LDC));
						}
					}
				}

				@Override
				public void visitFieldInsn(int opcode, String owner, String name, String desc) {
					String fieldClassName = owner.replace('/', '.');
					{
						FieldReference ref = new FieldReference(fieldClassName, name, desc);

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
					{
						ClassReference ref = new ClassReference(fieldClassName);

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
					{
						ClassReference ref = new ClassReference(Type.getType(desc).getClassName());

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
				}

				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
					String methodClassName = owner.replace('/', '.');
					{
						MethodReference ref = new MethodReference(methodClassName, name, desc);

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
					{
						ClassReference ref = new ClassReference(methodClassName);

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
					{
						ClassReference ref = new ClassReference(Type.getReturnType(desc).getClassName());

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
					for(Type argumentType : Type.getArgumentTypes(desc)) {
						ClassReference ref = new ClassReference(argumentType.getClassName());

						SearchFunction function = apply(ref);
						if(function != null)
							resultFound(new MethodInstructionSearchResult(function, ref, source, className, methodName, methodDescriptor, opcode));
					}
				}
			};
		}

		private void resultFound(SearchResult result) {
			this.result = result;
			state = SearchState.FOUND;
			awaitNextSearch();
		}

		private void awaitNextSearch() throws SearchStoppedException {
			notifyAll();
			while(state != SearchState.SEARCHING) {
				try {
					wait(2500);
				} catch(InterruptedException exception) {}
				if(state == SearchState.STOPPED)
					throw new SearchStoppedException();
			}
			result = null;
		}

		public synchronized SearchResult next() {
			if(!thread.isAlive() || state == SearchState.NOT_FOUND || state == SearchState.STOPPED)
				return null;
			state = SearchState.SEARCHING;

			notifyAll();
			while(thread.isAlive() && state == SearchState.SEARCHING) {
				try {
					wait(2500);
				} catch(InterruptedException exception) {}
			}

			return result;
		}

		public void stop() {
			state = SearchState.STOPPED;
			thread.interrupt();
		}
	}
}
