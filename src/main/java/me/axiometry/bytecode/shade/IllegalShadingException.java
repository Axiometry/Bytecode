package me.axiometry.bytecode.shade;

@SuppressWarnings("serial")
public class IllegalShadingException extends RuntimeException {
	public IllegalShadingException() {
	}

	public IllegalShadingException(String message) {
		super(message);
	}

	public IllegalShadingException(Throwable cause) {
		super(cause);
	}

	public IllegalShadingException(String message, Throwable cause) {
		super(message, cause);
	}

	public IllegalShadingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
