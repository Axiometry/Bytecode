package me.axiometry.bytecode.shade;

import java.util.Map;

import me.axiometry.bytecode.ClassSource;

import com.google.common.collect.ImmutableMap;

public interface ClassShader {
	public void addSource(ClassSource source);
	public void removeSource(ClassSource source);
	public ClassSource[] getSources();

	public ShadingResult shade(ShadeFunction function);

	public interface ShadingResult {
		public ShadeFunction getShadeFunction();

		public ShadedClassSource[] getShadedClasses();
		public int getModifications();
		public int getModifications(Class<? extends Identifier> type);
	}

	public static final class ShadedClassSource implements ClassSource {
		private final ClassSource source;
		private final Map<String, byte[]> classes;

		public ShadedClassSource(ClassSource source, Map<String, byte[]> classes) {
			this.source = source;
			this.classes = ImmutableMap.copyOf(classes);
		}
		public ClassSource getOriginalSource() {
			return source;
		}

		@Override
		public Map<String, byte[]> getClasses() {
			return classes;
		}
	}
}
