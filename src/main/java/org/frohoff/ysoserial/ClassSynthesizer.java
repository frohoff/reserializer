package org.frohoff.ysoserial;

import static java.io.ObjectStreamConstants.SC_EXTERNALIZABLE;
import static java.io.ObjectStreamConstants.SC_SERIALIZABLE;

import java.io.Externalizable;
import java.io.ObjectStreamConstants;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.SerialVersionUID;
import net.mcmanus.eamonn.serialysis.SerialScan.FieldDesc;
import net.mcmanus.eamonn.serialysis.SerialScan.ObjectClassDesc;

public class ClassSynthesizer {
	private static final String OBJECT_CONTENTS_FIELD_NAME = "_ysoserial_contents";

	public static Class<?> synthesize(ObjectClassDesc desc) throws NoSuchFieldException, NotFoundException, CannotCompileException  {
		try {
			return Class.forName(desc.getName()); // try to load normally one more time
		} catch (Exception e) {}
		
		final ObjectClassDesc superClassDesc = desc.getSuperClassDesc();		
		final FieldDesc[] fields = desc.getFields();
		
		final ClassPool pool = ClassPool.getDefault();
		final CtClass cc = pool.makeClass(desc.getName(), superClassDesc != null ? pool.getCtClass(superClassDesc.getName()) : null);
		
		cc.addInterface(pool.getCtClass(Doppelganger.class.getName()));
		
		// add field for object contents
		cc.addField(new CtField(pool.getCtClass(ObjectContent.class.getName()), OBJECT_CONTENTS_FIELD_NAME, cc){{
			setModifiers(Modifier.TRANSIENT | Modifier.PRIVATE);
		}});

		// handle serializable
		if ((desc.getFlags() & SC_SERIALIZABLE) != 0) {
			cc.addInterface(pool.getCtClass(Serializable.class.getName()));
			if ((desc.getFlags() & ObjectStreamConstants.SC_WRITE_METHOD) != 0) {
				cc.addMethod(CtMethod.make(
					"private void readObject(java.io.ObjectInputStream in){" 
						+ OBJECT_CONTENTS_FIELD_NAME + " = " + ObjectContent.class.getName() + ".read(in)"   
					+ ";}", cc));
				cc.addMethod(CtMethod.make(
						"private void writeObject(java.io.ObjectOutputStream out) {"
							+ ObjectContent.class.getName() + ".write(" + OBJECT_CONTENTS_FIELD_NAME + ",out)"					
						+";}", cc));				
			}
		}
		
		// handle externalizable
		if ((desc.getFlags() & SC_EXTERNALIZABLE) != 0) {
			cc.addInterface(pool.getCtClass(Externalizable.class.getName()));
			
			cc.addMethod(CtMethod.make(
					"public void readExternal(java.io.ObjectInput in) {"
						+ OBJECT_CONTENTS_FIELD_NAME + " = " + ObjectContent.class.getName() + ".read(in)"					
					+";}", cc));
			cc.addMethod(CtMethod.make(
					"public void writeExternal(java.io.ObjectOutput out) {"
						+ ObjectContent.class.getName() + ".write(" + OBJECT_CONTENTS_FIELD_NAME + ",out)"					
					+";}", cc));
		}
		
		// handle non-transient fields
		for (final FieldDesc field : fields) {
			cc.addField(new CtField(pool.getCtClass(field.getTypeName()), field.getName(), cc));
		}		
		
		// handle serialVersionUID
		if (desc.getSerialVersionUID() != null && (desc.getFlags() & (SC_SERIALIZABLE | SC_EXTERNALIZABLE)) != 0) {			
			cc.addField(new CtField(CtClass.longType,"serialVersionUID", cc){{
				setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
			}}, CtField.Initializer.constant(desc.getSerialVersionUID()));			
		} else {
			SerialVersionUID.setSerialVersionUID(cc);
		}		
		
		// handle enums
		if (superClassDesc != null && superClassDesc.getName().equals("java.lang.Enum")) {
			String name = cc.getName();
			CtMethod method = CtMethod.make("public static " + name + "[] values() { return new " + name + "[0]; }" , cc);
			cc.addMethod(method);
			
			cc.setModifiers(cc.getModifiers() | Modifier.ENUM);
		}
		
		// create class		
		Class<?> clazz = cc.toClass(); 
		
		// dynamically add enum values
		if (Enum.class.isAssignableFrom(clazz)) {
			addEnumValues((Class<? extends Enum>) clazz, desc.getEnumValues().toArray(new String[0]));
		}
		
		return clazz;
	}
	
	public static <T extends Enum> void addEnumValues(Class<T> clazz, String ... values ) {
		try {
			Constructor con = clazz.getDeclaredConstructors()[0];
			Method[] methods = con.getClass().getDeclaredMethods();
			for (Method m : methods) {
				if (m.getName().equals("acquireConstructorAccessor")) {
					m.setAccessible(true);
					m.invoke(con, new Object[0]);
				}
			}
			Field[] fields = con.getClass().getDeclaredFields();
			Object ca = null;
			for (Field f : fields) {
				if (f.getName().equals("constructorAccessor")) {
					f.setAccessible(true);
					ca = f.get(con);
				}
			}
			Method newInstanceMethod = ca.getClass().getMethod("newInstance", new Class[] { Object[].class });
			newInstanceMethod.setAccessible(true);
			Method enumConstantDictionaryMethod = Class.class.getDeclaredMethod("enumConstantDirectory", new Class<?>[0]);
			enumConstantDictionaryMethod.setAccessible(true);
			
			Map<String,T> dictionary = (Map<String, T>) enumConstantDictionaryMethod.invoke(clazz, new Object[0]);
			
			for (String value : values) {
				T instance = (T) newInstanceMethod.invoke(ca, new Object[] { new Object[] { value, Integer.MAX_VALUE } });
				dictionary.put(instance.name(), instance);			
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
