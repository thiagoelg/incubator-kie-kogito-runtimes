package org.drools.base;

/*
 * Copyright 2006 Alexander Bagerman
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.drools.RuntimeDroolsException;
import org.drools.asm.ClassWriter;
import org.drools.asm.Label;
import org.drools.asm.MethodVisitor;
import org.drools.asm.Opcodes;

/**
 * 
 * @author Alexander Bagerman
 * 
 */

public class ClassFieldExtractorFactory {

	private static final String GETTER = "get";

	private static final String BOOLEAN_GETTER = "is";

	private static final String BASE_PACKAGE = "org/drools/base";

	private static final String BASE_EXTRACTOR = "org/drools/base/BaseClassFieldExtractor";

	public static BaseClassFieldExtractor getClassFieldExtractor(Class clazz,
			String fieldName) {
		try {
			Class fieldType = getFieldType(clazz, fieldName);
			String originalClassName = clazz.getName().replace('.', '/');
			String getterName = getGetterName(clazz, fieldName);
			String className = BASE_PACKAGE + "/" + originalClassName + "$"
					+ getterName;
			String typeName = getTypeName(fieldType);
			// generating byte array to create target class
			byte[] bytes = dump(originalClassName, className, getterName,
					typeName, fieldType);
			// use bytes to get a class 
			ByteArrayClassLoader classLoader = new ByteArrayClassLoader(Thread
					.currentThread().getContextClassLoader());
			Class newClass = classLoader.defineClass(className
					.replace('/', '.'), bytes);
			// instantiating target class
			Object[] params = { clazz, fieldName };
			return (BaseClassFieldExtractor) newClass.getConstructors()[0]
					.newInstance(params);
		} catch (Exception e) {
			throw new RuntimeDroolsException(e);
		}
	}

	protected static Class getFieldType(Class clazz, String name)
			throws IntrospectionException {
		Class fieldType = null;
		PropertyDescriptor[] descriptors = Introspector.getBeanInfo(clazz)
				.getPropertyDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			if (descriptors[i].getName().equals(name)) {
				fieldType = descriptors[i].getPropertyType();
				break;
			}
		}

		return fieldType;
	}

	private static byte[] dump(String originalClassName, String className,
			String getterName, String typeName, Class fieldType)
			throws Exception {

		ClassWriter cw = new ClassWriter(false);
		MethodVisitor mv;

		cw.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
				className, null, BASE_EXTRACTOR, null);

		cw.visitSource(null, null);

		// constractor
		{
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
					"(Ljava/lang/Class;Ljava/lang/String;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(10, l0);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitVarInsn(Opcodes.ALOAD, 2);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, BASE_EXTRACTOR, "<init>",
					"(Ljava/lang/Class;Ljava/lang/String;)V");
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLineNumber(11, l1);
			mv.visitInsn(Opcodes.RETURN);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitLocalVariable("this", "L" + className + ";", null, l0, l2,
					0);
			mv
					.visitLocalVariable("clazz", "Ljava/lang/Class;", null, l0,
							l2, 1);
			mv.visitLocalVariable("fieldName", "Ljava/lang/String;", null, l0,
					l2, 2);
			mv.visitMaxs(3, 3);
			mv.visitEnd();
		}

		// for primitive it's different because we special characters for 
		// return types and create corresponding Objects (e.g. int -> Integer, boolean -> Boolean, ..)
		if (fieldType.isPrimitive()) {
			String primitiveTypeTag = getPrimitiveTag(fieldType);

			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getValue",
					"(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(14, l0);
			mv.visitTypeInsn(Opcodes.NEW, typeName);
			mv.visitInsn(Opcodes.DUP);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitTypeInsn(Opcodes.CHECKCAST, originalClassName);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, originalClassName,
					getterName, "()" + primitiveTypeTag);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, typeName, "<init>", "("
					+ primitiveTypeTag + ")V");
			mv.visitInsn(Opcodes.ARETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1,
					0);
			mv.visitLocalVariable("object", "Ljava/lang/Object;", null, l0, l1,
					1);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		} else {
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getValue",
					"(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(15, l0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitTypeInsn(Opcodes.CHECKCAST, originalClassName);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, originalClassName,
					getterName, "()L" + typeName + ";");
			mv.visitInsn(Opcodes.ARETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1,
					0);
			mv.visitLocalVariable("object", "Ljava/lang/Object;", null, l0, l1,
					1);
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}

	private static String getGetterName(Class clazz, String fieldName) {
		try {
			String getterName = null;
			boolean found = false;
			String pName = null;
			Method[] methods = clazz.getMethods();
			for (int i = 0; i < methods.length && !found; i++) {
				if ((methods[i].getModifiers() & Modifier.PUBLIC) != 0) {
					if (methods[i].getName().startsWith(GETTER)
							&& methods[i].getReturnType() != void.class
							&& methods[i].getParameterTypes().length == 0) {
						pName = Introspector.decapitalize(methods[i].getName()
								.substring(3));

						found = true;
					} else if (methods[i].getName().startsWith(BOOLEAN_GETTER)
							&& methods[i].getReturnType() == boolean.class
							&& methods[i].getParameterTypes().length == 0) {
						pName = Introspector.decapitalize(methods[i].getName()
								.substring(2));
						found = true;
					}
				}
				if (found && pName.equals(fieldName)) {
					getterName = methods[i].getName();
				} else {
					found = false;
				}

			}
			if (!found) {
				throw new RuntimeDroolsException(
						"Can not find name for the getter " + clazz.getName()
								+ "." + fieldName);
			}
			return getterName;

		} catch (Exception e) {
			throw new RuntimeDroolsException(e);
		}
	}

	protected static ClassObjectType getClassObjectType(Class fieldType)
			throws IntrospectionException {
		Class returnClass = null;
		// autobox primitives
		if (fieldType.isPrimitive()) {
			if (fieldType == char.class) {
				returnClass = Character.class;
			} else if (fieldType == byte.class) {
				returnClass = Byte.class;
			} else if (fieldType == short.class) {
				returnClass = Short.class;
			} else if (fieldType == int.class) {
				returnClass = Integer.class;
			} else if (fieldType == long.class) {
				returnClass = Long.class;
			} else if (fieldType == float.class) {
				returnClass = Float.class;
			} else if (fieldType == double.class) {
				returnClass = Double.class;
			} else if (fieldType == boolean.class) {
				returnClass = Boolean.class;
			}
		} else {
			returnClass = fieldType;
		}

		return new ClassObjectType(returnClass);
	}

	private static String getTypeName(Class fieldType) {
		String ret = null;

		if (fieldType.isPrimitive()) {
			if (fieldType == char.class) {
				ret = "java/lang/Character";
			} else if (fieldType == byte.class) {
				ret = "java/lang/Byte";
			} else if (fieldType == short.class) {
				ret = "java/lang/Short";
			} else if (fieldType == int.class) {
				ret = "java/lang/Integer";
			} else if (fieldType == long.class) {
				ret = "java/lang/Long";
			} else if (fieldType == float.class) {
				ret = "java/lang/Float";
			} else if (fieldType == double.class) {
				ret = "java/lang/Double";
			} else if (fieldType == boolean.class) {
				ret = "java/lang/Boolean";
			}
		} else {
			ret = fieldType.getName().replace('.', '/');
		}

		return ret;
	}

	private static String getPrimitiveTag(Class fieldType) {
		String ret = null;
		if (fieldType == char.class) {
			ret = "C";
		} else if (fieldType == byte.class) {
			ret = "B";
		} else if (fieldType == short.class) {
			ret = "S";
		} else if (fieldType == int.class) {
			ret = "I";
		} else if (fieldType == long.class) {
			ret = "J";
		} else if (fieldType == float.class) {
			ret = "F";
		} else if (fieldType == double.class) {
			ret = "D";
		} else if (fieldType == boolean.class) {
			ret = "Z";
		}

		return ret;
	}

	/**
	 * Simple classloader
	 * @author Michael Neale
	 */
	static class ByteArrayClassLoader extends ClassLoader {
		public ByteArrayClassLoader(ClassLoader parent) {
			super(parent);
		}

		public Class defineClass(String name, byte[] bytes) {
			return defineClass(name, bytes, 0, bytes.length);
		}
	}
}
