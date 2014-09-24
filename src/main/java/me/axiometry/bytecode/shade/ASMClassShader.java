package me.axiometry.bytecode.shade;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import me.axiometry.bytecode.ClassSource;

import org.objectweb.asm.*;

import com.google.common.base.Strings;
import com.google.common.cache.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class ASMClassShader extends AbstractClassShader {
	@Override
	public ShadingResult shade(ShadeFunction function) {
		return new ShadingProcessor(function).shade();
	}

	private final class ShadingProcessor {
		private final ShadeFunction function;
		private final Cache<Identifier, Identifier> shadedIdentifiers;
		private final Map<Class<? extends Identifier>, AtomicInteger> modificationCounters;

		public ShadingProcessor(ShadeFunction function) {
			this.function = function;

			shadedIdentifiers = CacheBuilder.newBuilder().maximumSize(2000).expireAfterAccess(3, TimeUnit.SECONDS).build();
			modificationCounters = new HashMap<>();
		}

		private ShadingResult shade() {
			List<ShadedClassSource> shadedSources = new ArrayList<>();

			for(final ClassSource source : getSources()) {
				Map<String, byte[]> classes = ImmutableMap.copyOf(source.getClasses());
				Map<String, byte[]> shadedClasses = new HashMap<>();

				for(Map.Entry<String, byte[]> classData : classes.entrySet()) {
					final String className = classData.getKey();
					ClassReader reader = new ClassReader(classData.getValue());
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

					ClassVisitor visitor = createClassVisitor(writer, source, className);
					reader.accept(visitor, 0);

					shadedClasses.put(className, writer.toByteArray());
				}

				shadedSources.add(new ShadedClassSource(source, shadedClasses));
			}

			Map<Class<? extends Identifier>, Integer> modifications = modificationCounters.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));

			shadedIdentifiers.invalidateAll();
			modificationCounters.clear();

			return new ASMShadingResult(function, shadedSources.toArray(new ShadedClassSource[shadedSources.size()]), modifications);
		}

		private ClassVisitor createClassVisitor(ClassWriter writer, final ClassSource source, final String className) {
			return new ClassVisitor(Opcodes.ASM5, writer) {
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					name = shade(new ClassIdentifier(name)).getClassName();
					if(signature != null)
						signature = SignatureShadingUtil.shadeClassSignature(signature, ShadingProcessor.this::shade);
					superName = shade(new ClassIdentifier(superName)).getClassName();
					for(int i = 0; i < interfaces.length; i++)
						interfaces[i] = shade(new ClassIdentifier(interfaces[i])).getClassName();

					super.visit(version, access, name, signature, superName, interfaces);
				}

				@Override
				public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
					name = shade(new FieldIdentifier(className, name, desc)).getFieldName();
					desc = shadeType(Type.getType(desc));
					if(signature != null)
						signature = SignatureShadingUtil.shadeFieldSignature(signature, ShadingProcessor.this::shade);

					return new FieldVisitor(Opcodes.ASM5, super.visitField(access, name, desc, signature, value)) {
						@Override
						public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
							desc = identifier2Desc(shade(desc2Identifier(desc)));

							return new AnnotationVisitor(Opcodes.ASM5, super.visitAnnotation(desc, visible)) {
								@Override
								public void visit(String name, Object value) {
									super.visit(name, value);
								}
							};
						}
					};
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					name = shade(new MethodIdentifier(className, name, desc)).getMethodName();
					desc = shadeType(Type.getMethodType(desc));
					if(signature != null)
						signature = SignatureShadingUtil.shadeMethodSignature(signature, ShadingProcessor.this::shade);
					if(exceptions != null) {
						exceptions = Arrays.copyOf(exceptions, exceptions.length);
						for(int i = 0; i < exceptions.length; i++)
							exceptions[i] = shadeType(Type.getType("L" + exceptions[i] + ";"));
					}

					return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
						@Override
						public void visitFieldInsn(int opcode, String owner, String name, String desc) {
							super.visitFieldInsn(opcode, owner, name, desc);
						}

						@Override
						public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
							super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
						}

						@Override
						public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
							super.visitLocalVariable(name, desc, signature, start, end, index);
						}

						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
							super.visitMethodInsn(opcode, owner, name, desc, itf);
						}

						@Override
						public void visitTypeInsn(int opcode, String type) {
							System.out.println("Found type: " + type);
							if(type.startsWith("["))
								type = shadeType(Type.getType(type));
							else
								type = shadeType(Type.getType("L" + type + ";"));

							super.visitTypeInsn(opcode, type);
						}

						@Override
						public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
							//if(type != null)
							//	type = identifier2Desc(shade(desc2Identifier(type)));

							super.visitTryCatchBlock(start, end, handler, type);
						}
					};
				}

				private String shadeType(Type t) {
					if(t.getSort() == Type.METHOD) {
						String desc = "(";
						for(Type argT : t.getArgumentTypes())
							desc += shadeType(argT);
						desc += ")" + shadeType(t.getReturnType());
						return desc;
					} else if(t.getSort() == Type.OBJECT)
						return identifier2Desc(shade(new ClassIdentifier(t.getClassName())));
					else if(t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT)
						return Strings.repeat("[", t.getDimensions()) + identifier2Desc(shade(new ClassIdentifier(t.getElementType().getClassName())));
					return t.getDescriptor();
				}

				private ClassIdentifier desc2Identifier(String desc) {
					desc = desc.substring(1, desc.length() - 1);
					desc = desc.replace('/', '.');

					return new ClassIdentifier(desc);
				}

				private String identifier2Desc(ClassIdentifier identifier) {
					String desc = identifier.getClassName();
					desc = desc.replace('.', '/');
					desc = "L" + desc + ";";

					return desc;
				}
			};
		}
		@SuppressWarnings("unchecked")
		private <T extends Identifier> T shade(T identifier) {
			try {
				return (T) shadedIdentifiers.get(identifier, () -> {
					Identifier newIdentifier = function.shade(identifier);
					if(!identifier.canShadeInto(newIdentifier))
						throw new IllegalShadingException();
					if(identifier.equals(newIdentifier))
						throw new RuntimeException();
					return newIdentifier;
				});
			} catch(UncheckedExecutionException | ExecutionException exception) {
				if(exception.getCause() instanceof IllegalShadingException)
					throw (IllegalShadingException) exception.getCause();
				return identifier;
			}
		}
	}

	private static final class ASMShadingResult implements ShadingResult {
		private final ShadeFunction function;

		private final ShadedClassSource[] shadedClasses;
		private final Map<Class<? extends Identifier>, Integer> modifications;
		private final int totalModifications;

		public ASMShadingResult(ShadeFunction function, ShadedClassSource[] shadedClasses, Map<Class<? extends Identifier>, Integer> modifications) {
			this.function = function;
			this.shadedClasses = shadedClasses.clone();
			this.modifications = ImmutableMap.copyOf(modifications);

			int totalModifications = 0;
			for(int n : modifications.values())
				totalModifications += n;
			this.totalModifications = totalModifications;
		}

		@Override
		public ShadeFunction getShadeFunction() {
			return function;
		}

		@Override
		public ShadedClassSource[] getShadedClasses() {
			return shadedClasses.clone();
		}

		@Override
		public int getModifications() {
			return totalModifications;
		}

		@Override
		public int getModifications(Class<? extends Identifier> type) {
			return modifications.getOrDefault(type, 0);
		}
	}
}
