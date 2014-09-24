package me.axiometry.bytecode.search;

import java.util.regex.Pattern;

public final class SearchFunctions {
	private SearchFunctions() {
	}

	// --- String Search Functions ---
	public static SearchFunction string(final String s) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof String && constant.equals(s);
			}
		};
	}

	public static SearchFunction stringContains(final String s) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof String && constant.toString().contains(s);
			}
		};
	}

	public static SearchFunction stringStartsWith(final String s) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof String && constant.toString().startsWith(s);
			}
		};
	}

	public static SearchFunction stringEndsWith(final String s) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof String && constant.toString().endsWith(s);
			}
		};
	}

	public static SearchFunction stringMatches(final String regex) {
		final Pattern pattern = Pattern.compile(regex);
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof String && pattern.matcher(constant.toString()).matches();
			}
		};
	}

	// --- Number Search Functions ---
	public static SearchFunction number(final int i) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof Integer && i == ((Integer) constant).intValue();
			}
		};
	}

	public static SearchFunction number(final long l) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof Long && l == ((Long) constant).longValue();
			}
		};
	}

	public static SearchFunction number(final double d) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof Double && d == ((Double) constant).doubleValue();
			}
		};
	}

	public static SearchFunction number(final float f) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof Float && f == ((Float) constant).floatValue();
			}
		};
	}

	public static SearchFunction number(final short s) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof Short && s == ((Short) constant).shortValue();
			}
		};
	}

	public static SearchFunction number(final byte b) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				return constant instanceof Byte && b == ((Byte) constant).byteValue();
			}
		};
	}

	// --- Reference Search Functions ---
	public static SearchFunction classReference(final String className) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				if(!(constant instanceof ClassReference))
					return false;
				ClassReference reference = (ClassReference) constant;
				if(!className.equals(reference.getClassName()))
					return false;
				return true;
			}
		};
	}

	public static SearchFunction fieldReference(final String className, final String fieldName) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				if(!(constant instanceof FieldReference))
					return false;
				FieldReference reference = (FieldReference) constant;
				if(!className.equals(reference.getClassName()))
					return false;
				if(!fieldName.equals(reference.getFieldName()))
					return false;
				return true;
			}
		};
	}

	public static SearchFunction fieldReference(final String className, final String fieldName, final String fieldDescriptor) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				if(!(constant instanceof FieldReference))
					return false;
				FieldReference reference = (FieldReference) constant;
				if(!className.equals(reference.getClassName()))
					return false;
				if(!fieldName.equals(reference.getFieldName()))
					return false;
				if(!fieldDescriptor.equals(reference.getFieldDescriptor()))
					return false;
				return true;
			}
		};
	}

	public static SearchFunction methodReference(final String className, final String methodName) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				if(!(constant instanceof MethodReference))
					return false;
				MethodReference reference = (MethodReference) constant;
				if(!className.equals(reference.getClassName()))
					return false;
				if(!methodName.equals(reference.getMethodName()))
					return false;
				return true;
			}
		};
	}

	public static SearchFunction methodReference(final String className, final String methodName, final String methodDescriptor) {
		return new SearchFunction() {
			@Override
			public boolean matches(Object constant) {
				if(!(constant instanceof MethodReference))
					return false;
				MethodReference reference = (MethodReference) constant;
				if(!className.equals(reference.getClassName()))
					return false;
				if(!methodName.equals(reference.getMethodName()))
					return false;
				if(!methodDescriptor.equals(reference.getMethodDescriptor()))
					return false;
				return true;
			}
		};
	}
}