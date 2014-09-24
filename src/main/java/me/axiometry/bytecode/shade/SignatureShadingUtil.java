package me.axiometry.bytecode.shade;

import java.io.*;

public final class SignatureShadingUtil {
	public interface ShadingFunction {
		public <T extends Identifier> T shade(T identifier) throws IllegalShadingException;
	}

	private interface ShadingPipeFunction {
		public void shade(Reader reader, Writer writer, ShadingFunction shader) throws IOException;
	}

	private SignatureShadingUtil() {
		throw new UnsupportedOperationException();
	}

	public static String shadeClassSignature(String signature, ShadingFunction shader) {
		return shadeSignature(signature, shader, SignatureShadingUtil::shadeClassSignature);
	}

	public static String shadeFieldSignature(String signature, ShadingFunction shader) {
		return shadeSignature(signature, shader, SignatureShadingUtil::shadeFieldTypeSignature);
	}

	public static String shadeMethodSignature(String signature, ShadingFunction shader) {
		return shadeSignature(signature, shader, SignatureShadingUtil::shadeMethodSignature);
	}

	private static String shadeSignature(String signature, ShadingFunction shader, ShadingPipeFunction shaderPipe) {
		StringReader reader = new StringReader(signature);
		StringWriter writer = new StringWriter();
		try {
			shaderPipe.shade(reader, writer, shader);

			if(!signature.equals(writer.toString())) {
				//System.out.println("Shaded signature");
				//System.out.println("  From: " + signature);
				//System.out.println("    To: " + writer.toString());
			}

			return writer.toString();
		} catch(IllegalShadingException exception) {
			throw exception;
		} catch(Exception exception) {
			System.out.println("Failed to parse signature: " + signature + " / " + exception.toString());
			System.out.println("  " + writer.toString());
			exception.printStackTrace();
			return signature;
		}
	}

	// Signature                 := FormalTypeParameters? SuperclassSignature SuperinterfaceSignature*
	//   FormalTypeParameters    := < FormalTypeParameter+ >
	//   SuperclassSignature     := ClassTypeSignature
	//   SuperinterfaceSignature := ClassTypeSignature
	private static void shadeClassSignature(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		reader.mark(1);
		char c = read(reader);
		if(c == '<') {
			writer.write('<');
			while(true) {
				shadeFormalTypeParameter(reader, writer, shader);

				reader.mark(1);
				c = read(reader);
				if(c == '>')
					break;
				reader.reset();
			}
			writer.write('>');
		} else
			reader.reset();

		shadeClassTypeSignature(reader, writer, shader);

		while(true) {
			reader.mark(1);
			if(reader.read() == -1)
				break;
			reader.reset();

			shadeClassTypeSignature(reader, writer, shader);
		}
	}

	// Signature              := FormalTypeParameters ( TypeSignature* ) ReturnType ThrowsSignature*
	//   FormalTypeParameters := < FormalTypeParameter+ >
	//   ReturnType           := TypeSignature
	//                        |= VoidDescriptor
	//   ThrowsSignature      := ^ ClassTypeSignature
	//                        |= ^ TypeVariableSignature
	private static void shadeMethodSignature(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		reader.mark(1);
		char c = read(reader);
		if(c == '<') {
			writer.write('<');
			while(true) {
				shadeFormalTypeParameter(reader, writer, shader);

				reader.mark(1);
				c = read(reader);
				if(c == '>')
					break;
				reader.reset();
			}
			writer.write('>');
		} else
			reader.reset();

		c = read(reader);
		if(c == '(') {
			writer.write('(');
			while(true) {
				reader.mark(1);
				c = read(reader);
				if(c == ')')
					break;
				reader.reset();

				shadeTypeSignature(reader, writer, shader);
			}
			writer.write(')');
		} else
			throw new IOException();

		reader.mark(1);
		c = read(reader);
		if(c != 'V') {
			reader.reset();

			shadeTypeSignature(reader, writer, shader);
		} else
			writer.write('V');

		while(true) {
			int i = reader.read();
			if(i != '^')
				if(i != -1)
					throw new IOException();
				else
					break;

			reader.mark(1);
			c = read(reader);
			reader.reset();

			if(c == 'T') {
				writer.write(read(reader));
				writer.write(readIdentifier(reader));
				writer.write(c = read(reader));
				if(c != ';')
					throw new IOException();
			} else
				shadeClassTypeSignature(reader, writer, shader);
		}
	}

	// FormalTypeParameter := Identifier ClassBound InterfaceBound*
	//   ClassBound        := : FieldTypeSignature?
	//   InterfaceBound    := : FieldTypeSignature
	private static void shadeFormalTypeParameter(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		writer.write(readIdentifier(reader));

		char c = read(reader);
		if(c != ':')
			throw new IOException();
		do {
			writer.write(':');

			reader.mark(1);
			c = read(reader);
			reader.reset();
			if(c == 'L' || c == '[' || c == 'T')
				shadeFieldTypeSignature(reader, writer, shader);

			reader.mark(1);
			c = read(reader);
		} while(c == ':');
		reader.reset();
	}

	// TypeSignature := FieldTypeSignature
	//               |= BaseType
	private static void shadeTypeSignature(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		reader.mark(1);

		char c = read(reader);
		if(c == 'L' || c == '[' || c == 'T') {
			reader.reset();
			shadeFieldTypeSignature(reader, writer, shader);
		} else
			writer.write(c);
	}

	// FieldTypeSignature      := ClassTypeSignature
	//                         |= ArrayTypeSignature
	//                         |= TypeVariableSignature
	//   ArrayTypeSignature    := [+ TypeSignature
	//   TypeVariableSignature := T Identifier ;
	private static void shadeFieldTypeSignature(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		reader.mark(1);

		char c = read(reader);
		if(c == 'L') {
			reader.reset();
			shadeClassTypeSignature(reader, writer, shader);
		} else if(c == '[') {
			writer.write('[');
			reader.mark(1);
			while((c = read(reader)) == '[') {
				writer.write('[');
				reader.mark(1);
			}
			reader.reset();
			shadeTypeSignature(reader, writer, shader);
		} else if(c == 'T') {
			writer.write('T');
			writer.write(readIdentifier(reader));
			writer.write(c = read(reader));
			if(c != ';')
				throw new IOException();
		} else
			throw new IOException();
	}

	// ClassTypeSignature         := L PackageSpecifier? SimpleClassTypeSignature ClassTypeSignatureSuffix* ;
	//   PackageSpecifier         := Identifier / PackageSpecifier*
	//   SimpleClassTypeSignature := Identifier TypeArguments?
	//     TypeArguments          := < TypeArgument+ >
	//   ClassTypeSignatureSuffix := . SimpleClassTypeSignature
	private static void shadeClassTypeSignature(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		char c = read(reader);
		if(c != 'L')
			throw new IOException();
		writer.write('L');

		String className = "";
		do {
			className += readIdentifier(reader);
			c = read(reader);
			if(c == '/')
				className += '/';
		} while(c == '/');

		if(c == '.') {
			className += "$" + readIdentifier(reader);
			while(true) {
				reader.mark(1);
				if(read(reader) != '.') {
					reader.reset();
					break;
				} else
					className += "$" + readIdentifier(reader);
			}
			c = read(reader);
		}

		className = shader.shade(new ClassIdentifier(className.replace('/', '.'))).getClassName().replace('.', '/');
		writer.write(className);

		writer.write(c);
		if(c == '<') {
			while(true) {
				reader.mark(1);
				c = read(reader);
				if(c != '>')
					reader.reset();
				else
					break;

				shadeTypeArgument(reader, writer, shader);
			}
			writer.write('>');

			c = read(reader);
			writer.write(c);
		}
		if(c != ';')
			throw new IOException();
	}

	// TypeArgument      := WildcardIndicator? FieldTypeSignature
	//                   |= *
	// WildcardIndicator := +
	//                   |= -
	private static void shadeTypeArgument(Reader reader, Writer writer, ShadingFunction shader) throws IOException {
		reader.mark(1);
		char c = read(reader);
		if(c == '*' || c == '+' || c == '-') {
			writer.write(c);
			if(c == '*')
				return;
		} else
			reader.reset();

		shadeFieldTypeSignature(reader, writer, shader);
	}

	private static String readIdentifier(Reader reader) throws IOException {
		StringBuilder builder = new StringBuilder();

		char c = read(reader);
		if(!Character.isJavaIdentifierStart(c))
			throw new IOException();
		builder.append(c);

		reader.mark(1);
		while(Character.isJavaIdentifierPart(c = read(reader))) {
			builder.append(c);
			reader.mark(1);
		}
		reader.reset();

		return builder.toString();
	}

	private static char read(Reader reader) throws IOException {
		int c = reader.read();
		if(c == -1)
			throw new IOException();
		return (char) c;
	}
}
